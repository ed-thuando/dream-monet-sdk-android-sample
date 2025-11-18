# Dream Monet Ads SDK

A comprehensive Android ads SDK that provides a unified interface for multiple ad networks (AdMob and AppLovin MAX) with Firebase Remote Config integration, GDPR/CCPA consent management, and coroutines support.

## Features

- **Multi-Network Support**: Unified interface for AdMob and AppLovin MAX
- **Firebase Remote Config**: Dynamic ad configuration without app updates
- **GDPR/CCPA Compliance**: Built-in consent management using Google UMP
- **Coroutines Integration**: Full async/await support with Kotlin coroutines
- **Ad Types**: Banner, Interstitial, Rewarded Video, Native, and App Open ads
- **Simple API**: Easy-to-use Common API for all ad types

## Table of Contents

1. [Installation](#installation)
    - [Step 1: Configure Repository Access](#step-1-configure-repository-access)
    - [Step 2: Add Repository to settings.gradle.kts](#step-2-add-repository-to-settingsgradlekts)
    - [Step 3: Add Dependency](#step-3-add-dependency)
2. [SDK Initialization](#sdk-initialization)
    - [Application Class Setup](#application-class-setup)
    - [Initialize AdMob and MAX](#initialize-admob-and-max)
    - [Fetch Remote Config](#fetch-remote-config)
3. [Splash Screen Integration](#splash-screen-integration)
    - [Listen for Consent](#listen-for-consent)
    - [Listen for Remote Config Updates](#listen-for-remote-config-updates)
4. [Ad Implementation](#ad-implementation)
    - [Banner Ads](#banner-ads)
    - [Native Ads](#native-ads)
    - [Interstitial Ads](#interstitial-ads)
    - [Rewarded Ads](#rewarded-ads)
    - [App Open Ads](#app-open-ads)
5. [XML Layouts](#xml-layouts)
6. [Best Practices](#best-practices)

---

## Installation

### Step 1: Configure Repository Access

Create or edit your `gradle.properties` file (in project root or `~/.gradle/gradle.properties`):

```properties
# Nexus Repository Credentials
NEXUS_USERNAME=your_nexus_username
NEXUS_PASSWORD=your_nexus_password
```

**Important**: Shouldn't commit `gradle.properties` with real credentials to version control. Add it to `.gitignore`.

### Step 2: Add Repository to settings.gradle.kts

Add the Nexus repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Add Dream Monet SDK repository
        maven {
            url = uri("https://nexus.synthraai.tech/repository/maven-releases/")
            credentials {
                username = settings.providers.gradleProperty("NEXUS_USERNAME").orNull
                password = settings.providers.gradleProperty("NEXUS_PASSWORD").orNull
            }
        }
        maven {
            url = uri("https://nexus.synthraai.tech/repository/maven-snapshots/")
            credentials {
                username = settings.providers.gradleProperty("NEXUS_USERNAME").orNull
                password = settings.providers.gradleProperty("NEXUS_PASSWORD").orNull
            }
        }
        maven{ url = uri("https://maven.singular.net/") }
    }
}
```

### Step 3: Add Dependency

Add the SDK dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Dream Monet Ads SDK
    implementation("com.dream.monet:ads:1.0.0-alpha32-SNAPSHOT")

    // Required: Firebase (if not already added)
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
}
```

Also add the Google Services plugin to your app's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Add this
}
```

And in your project-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.3" apply false
}
```

Don't forget to add your `google-services.json` file to the `app/` directory.

---

## SDK Initialization

### Application Class Setup

## ⚠️ Important: SDK Initialization Flow

**Before using any ads functionality, you MUST follow this initialization sequence:**

1. **At Application Level**: After fetching Firebase Remote Config, always invoke:
   ```kotlin
   SDKManager.updateRemoteConfig(remoteConfig)
   ```

2. **At Splash Screen**: Register a listener and wait for the `onUpdatedRemoteConfig()` callback:
   ```kotlin
   override fun onUpdatedFirebaseRemoteConfig() {
       // Remote Config is now synced with the SDK
       // NOW you can proceed with consent gathering and loading ads
   }
   ```

3. **All ads actions** (loading, showing, etc.) should ONLY happen **after** the `onUpdatedRemoteConfig()` callback is triggered.

**Why this matters**: The SDK needs the Remote Config data to properly configure ad units, networks, and behavior. Loading ads before this callback completes may result in incorrect configuration or ads not loading properly.

---

Create an Application class to initialize the SDK. This should be done once when your app starts.

```kotlin
class MyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase Remote Config
        initializeFirebaseRemoteConfig()

        // 2. Initialize the Ads SDK
        initializeAdSDK()
    }

    private fun initializeFirebaseRemoteConfig() {
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        // Configure Remote Config settings
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour for production
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        Log.d("MyApp", "Firebase Remote Config initialized")
    }

    private fun initializeAdSDK() {
        applicationScope.launch {
            try {
                // Initialize SDK with AdMob and MAX
                val initResult = SDKManager.initialize(
                    this@DreamMonetApplication,
                    config = InitializeConfig.builder()
                        .enableAdMob(true)
                        .setMaxSdkKey(
                            "YOUR-MAX-SDK-KEY"
                        )
                        .setSingularKeys(
                            SingularKeys(
                                sdkKey = "your-singular-sdk-key",
                                sdkSecret = "your-singular-secret-key",
                            )
                        )
                        .setTestDeviceIds(
                            setOf(
                                "Your-device-id"
                            )
                        )
                        .build()
                )

                if (initResult.isSuccess) {
                    Log.d("MyApp", "SDK initialized successfully")
                } else {
                    Log.e("MyApp", "SDK initialization failed: ${initResult.exceptionOrNull()?.message}")
                }
                // Fetch and update remote config
                fetchAndUpdateRemoteConfig()
            } catch (e: Exception) {
                Log.e("MyApp", "Error initializing SDK: ${e.message}", e)
            }
        }
    }

    private suspend fun fetchAndUpdateRemoteConfig() {
        try {
            Log.d("MyApp", "Fetching Firebase Remote Config...")

            val fetchResult = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    firebaseRemoteConfig.fetchAndActivate()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("MyApp", "Remote config fetch successful")
                                continuation.resume(true)
                            } else {
                                Log.e("MyApp", "Remote config fetch failed")
                                continuation.resume(false)
                            }
                        }
                }
            }

            // Update SDK with new remote config
            // ⚠️ CRITICAL: This must be called after fetching Remote Config
            // The SDK will notify listeners via onUpdatedFirebaseRemoteConfig()
            SDKManager.updateRemoteConfig(firebaseRemoteConfig)

        } catch (e: Exception) {
            Log.e("MyApp", "Error fetching remote config: ${e.message}", e)
        }
    }
}
```

### Initialize AdMob and MAX

The SDK automatically initializes both AdMob and AppLovin MAX when you call `SDKManager.initialize()`:

- **AdMob**: Initialized automatically (no app ID required in code, it's read from AndroidManifest.xml)
- **AppLovin MAX**: Pass your SDK key to the `maxSdkKey` parameter, or pass `null` if not using MAX

**Add AdMob App ID to AndroidManifest.xml:**

```xml
<manifest>
    <application>
        ...
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
    </application>
</manifest>
```

---

## Splash Screen Integration

Use the Splash screen to handle consent gathering and wait for Remote Config updates before showing the main app.

### Listen for Consent

```kotlin
class SplashActivity : AppCompatActivity(), SDKInitializationListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Register for SDK initialization events
        SDKManager.addInitializationListener(this)
    }

    override fun onInitializationComplete(success: Boolean, errorMessage: String?) {
        if (success) {
            Log.d("Splash", "SDK initialized successfully")
        } else {
            Log.e("Splash", "SDK initialization failed: $errorMessage")
        }
    }

    override fun onUpdatedFirebaseRemoteConfig() {
        // ⚠️ CRITICAL: This callback is triggered after SDKManager.updateRemoteConfig() is called
        // Remote Config is now synced with the SDK
        Log.d("Splash", "Remote Config updated - SDK is ready")

        // NOW it's safe to gather consent and proceed with ads
        startConsentGathering()
    }

    private fun startConsentGathering() {
        lifecycleScope.launch {
            val consentResult = SDKManager.gatherConsent(
                activity = this@SplashActivity,
                testDeviceHashedId = "YOUR_TEST_DEVICE_ID", // Optional, for testing
                debugGeography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA // Optional
            )

            if (consentResult.isSuccess) {
                Log.d("Splash", "Consent gathered successfully")
            } else {
                Log.e("Splash", "Consent gathering failed")
            }

            // Navigate to main activity
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        SDKManager.removeInitializationListener(this)
    }
}
```

### Listen for Remote Config Updates

The `SDKInitializationListener` interface provides two callbacks:

1. **`onInitializationComplete(success: Boolean, errorMessage: String?)`**: Called when SDK initialization completes
2. **`onUpdatedFirebaseRemoteConfig()`**: Called when Firebase Remote Config is fetched and updated via `SDKManager.updateRemoteConfig()`

**⚠️ CRITICAL - Initialization Flow:**

1. At **Application level**: Fetch Remote Config → Call `SDKManager.updateRemoteConfig(remoteConfig)`
2. At **Splash Screen**: Wait for `onUpdatedFirebaseRemoteConfig()` callback
3. **After callback**: Proceed with consent gathering and loading ads

**DO NOT** load or show any ads before `onUpdatedFirebaseRemoteConfig()` is triggered. The SDK needs the Remote Config data to properly configure ad units, networks, and behavior.

---

## Ad Implementation

The SDK provides a simple Common API for all ad types. Each ad type has a corresponding class that handles loading and showing ads.

### Banner Ads

Banner ads are displayed inline within your app's layout.

#### Kotlin Code

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var bannerAd: CommonBannerAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create banner ad
        bannerAd = CommonBannerAd(
            key = "my_banner_key", // Unique key for Remote Config
            defaultUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ad unit
        )

        // Optional: Set listener
        bannerAd.setListener(object : BannerAdListener() {
            override fun onAdLoaded() {
                Log.d("Banner", "Ad loaded")
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e("Banner", "Ad failed to load: $error")
            }

            override fun onAdClicked() {
                Log.d("Banner", "Ad clicked")
            }
        })

        // Load and show banner ad
        lifecycleScope.launch {
            val container = findViewById<ViewGroup>(R.id.bannerAdContainer)
            bannerAd.show(this@MyActivity, container)
        }
    }
}
```

#### XML Layout

```xml
<FrameLayout
    android:id="@+id/bannerAdContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    android:background="#F5F5F5"
    android:minHeight="50dp" />
```

---

### Native Ads

Native ads match the look and feel of your app's content.

#### Kotlin Code

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var nativeAd: CommonNativeAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create native ad
        nativeAd = CommonNativeAd(
            key = "my_native_key", // Unique key for Remote Config
            defaultUnitId = "ca-app-pub-3940256099942544/2247696110", // Test ad unit
            layoutRes = R.layout.ad_unified // Your custom native ad layout
        )

        // Optional: Set listener
        nativeAd.setListener(object : NativeAdListener() {
            override fun onAdLoaded() {
                Log.d("Native", "Ad loaded")
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e("Native", "Ad failed to load: $error")
            }

            override fun onAdClicked() {
                Log.d("Native", "Ad clicked")
            }
        })

        // Load native ad
        lifecycleScope.launch {
            nativeAd.load(this@MyActivity)
        }
    }

    private fun showNativeAd() {
        lifecycleScope.launch {
            if (nativeAd.isReady()) {
                val container = findViewById<ViewGroup>(R.id.nativeAdContainer)
                nativeAd.show(this@MyActivity, container)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nativeAd.destroy()
    }
}
```

#### XML Layout

```xml
<FrameLayout
    android:id="@+id/nativeAdContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp" />
```

**Note**: You need to create a custom native ad layout. See the [XML Layouts](#xml-layouts) section below for a complete example.

---

### Interstitial Ads

Interstitial ads are full-screen ads that cover the interface of their host app. They're typically displayed at natural transition points in the flow of an app, such as between activities or during the pause between levels in a game.

#### Kotlin Code

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var interstitialAd: CommonInterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create interstitial ad
        interstitialAd = CommonInterstitialAd(
            key = "my_interstitial_key", // Unique key for Remote Config
            defaultUnitId = "ca-app-pub-3940256099942544/1033173712" // Test ad unit
        )

        // Load interstitial ad
        lifecycleScope.launch {
            interstitialAd.load(this@MyActivity)
        }

        // Show button
        findViewById<Button>(R.id.showInterstitialButton).setOnClickListener {
            showInterstitialAd()
        }
    }

    private fun showInterstitialAd() {
        if (interstitialAd.isReady()) {
            interstitialAd.show(
                activity = this,
                callback = object : InterstitialAdShowCallback {
                    override fun onAdShowed() {
                        Log.d("Interstitial", "Ad showed")
                    }

                    override fun onAdDismissed() {
                        Log.d("Interstitial", "Ad dismissed")
                        // Pre-load next ad
                        lifecycleScope.launch {
                            interstitialAd.load(this@MyActivity)
                        }
                    }

                    override fun onShowFailed(error: String) {
                        Log.e("Interstitial", "Ad show failed: $error")
                    }
                }
            )
        } else {
            Log.d("Interstitial", "Ad not ready yet")
        }
    }
}
```

---

### Rewarded Ads

Rewarded ads give users rewards (coins, extra lives, etc.) for watching video ads.

#### Kotlin Code

```kotlin
class MyActivity : AppCompatActivity() {

    private lateinit var rewardedAd: CommonRewardedAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create rewarded ad
        rewardedAd = CommonRewardedAd(
            key = "my_rewarded_key", // Unique key for Remote Config
            defaultUnitId = "ca-app-pub-3940256099942544/5224354917" // Test ad unit
        )

        // Load rewarded ad
        lifecycleScope.launch {
            rewardedAd.load(this@MyActivity)
        }

        // Show button
        findViewById<Button>(R.id.showRewardedButton).setOnClickListener {
            showRewardedAd()
        }
    }

    private fun showRewardedAd() {
        if (rewardedAd.isReady()) {
            rewardedAd.show(
                activity = this,
                timeoutMs = 0L, // Optional timeout
                callback = object : RewardedAdShowCallback {
                    override fun onAdShowed() {
                        Log.d("Rewarded", "Ad showed")
                    }

                    override fun onAdRewarded(isRewarded: Boolean) {
                        if (isRewarded) {
                            Log.d("Rewarded", "User earned reward!")
                            // Give reward to user
                            giveRewardToUser()
                        }
                    }

                    override fun onAdDismissed() {
                        Log.d("Rewarded", "Ad dismissed")
                        // Pre-load next ad
                        lifecycleScope.launch {
                            rewardedAd.load(this@MyActivity)
                        }
                    }

                    override fun onShowFailed(error: String) {
                        Log.e("Rewarded", "Ad show failed: $error")
                    }
                }
            )
        } else {
            Log.d("Rewarded", "Ad not ready yet")
        }
    }

    private fun giveRewardToUser() {
        // Your reward logic here
        Toast.makeText(this, "You earned 100 coins!", Toast.LENGTH_SHORT).show()
    }
}
```

---

### App Open Ads

App open ads are shown when users open or switch back to your app.

#### Kotlin Code

```kotlin
class AppOpenAdManager(private val application: Application) : LifecycleObserver {

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921" // Test ad unit
    }

    private var appOpenAd: CommonAppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                showAdIfAvailable()
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }
        })
    }

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        appOpenAd = CommonAppOpenAd("my_app_open_key", AD_UNIT_ID).apply {
            load(
                application,
                object : AdLoadListener {
                    override fun onAdLoaded(adId: String, adNetworkType: String) {
                        isLoadingAd = false
                    }

                    override fun onAdLoadFailed(adId: String, error: String, code: Int?, adNetworkType: String?) {
                        isLoadingAd = false
                        appOpenAd = null
                    }
                }
            )
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd?.isReady() == true
    }

    private fun showAdIfAvailable() {
        if (isShowingAd || !isAdAvailable()) {
            loadAd() // Pre-load for next time
            return
        }

        val activity = currentActivity ?: return

        isShowingAd = true
        appOpenAd?.show(
            activity,
            object : FullScreenAdListener() {
                override fun onAdDismissed(adId: String, adNetworkType: String) {
                    isShowingAd = false
                    appOpenAd = null
                    loadAd() // Pre-load the next ad
                }

                override fun onAdFailedToShow(adId: String, error: String, adNetworkType: String?) {
                    isShowingAd = false
                    appOpenAd = null
                    loadAd()
                }
            }
        )
    }
}
```

**Initialize in Application class:**

```kotlin
class MyApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()

        // ... SDK initialization code ...

        // Initialize App Open Ad Manager
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.loadAd()
    }
}
```

---

## XML Layouts

### Native Ad Layout Example

Create a custom native ad layout file (e.g., `res/layout/ad_unified.xml`):

```xml
<com.google.android.gms.ads.nativead.NativeAdView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/ad_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#FFFFFF"
        android:orientation="vertical"
        android:padding="8dp">

        <!-- Ad Badge and Options -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="#FFCC66"
                android:paddingStart="4dp"
                android:paddingEnd="4dp"
                android:text="Ad"
                android:textColor="#FFFFFF"
                android:textSize="12sp" />

            <FrameLayout
                android:id="@+id/options_view"
                android:layout_width="25dp"
                android:layout_height="25dp"
                android:layout_marginStart="4dp" />
        </LinearLayout>

        <!-- Ad Content -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <!-- Ad Icon -->
            <ImageView
                android:id="@+id/ad_app_icon"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:layout_marginEnd="8dp" />

            <!-- Ad Headline and Advertiser -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/ad_headline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="#000000"
                    android:textSize="16sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/ad_advertiser"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="#666666"
                    android:textSize="14sp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Ad Body -->
        <TextView
            android:id="@+id/ad_body"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textColor="#333333"
            android:textSize="14sp" />

        <!-- Ad Media -->
        <com.google.android.gms.ads.nativead.MediaView
            android:id="@+id/ad_media"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:layout_marginTop="8dp" />

        <!-- Ad Call to Action -->
        <Button
            android:id="@+id/ad_call_to_action"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textColor="#FFFFFF" />

    </LinearLayout>
</com.google.android.gms.ads.nativead.NativeAdView>
```

**Important Native Ad View IDs:**

The SDK automatically binds these view IDs to the native ad components:

- `ad_view`: Root NativeAdView (required)
- `ad_headline`: Ad headline text
- `ad_body`: Ad body text
- `ad_call_to_action`: Call to action button
- `ad_app_icon`: App icon image
- `ad_advertiser`: Advertiser name
- `ad_media`: Media view for images/videos
- `ad_stars`: Star rating (RatingBar for AdMob)
- `max_ad_stars`: Star rating (FrameLayout for MAX)
- `options_view`: Ad choices icon

---

## Best Practices

### 1. Initialize Early
Initialize the SDK in your Application class's `onCreate()` method to ensure it's ready before any activities start.

### 2. Pre-load Ads
Load ads before you need to show them to provide a better user experience:

```kotlin
// Load in onCreate
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
        rewardedAd.load(this@MyActivity)
    }
}

// Show later when needed
button.setOnClickListener {
    if (rewardedAd.isReady()) {
        rewardedAd.show(this, callback)
    }
}
```

### 3. Handle Lifecycle Events
Always handle activity lifecycle events for banner ads:

```kotlin
override fun onPause() {
    super.onPause()
    bannerAd.pause()
}

override fun onResume() {
    super.onResume()
    bannerAd.resume()
}

override fun onDestroy() {
    super.onDestroy()
    bannerAd.destroy()
}
```

### 4. Use Remote Config
The PO/BA is responsible for operating this part.

### 5. Test with Test Ad Units
Always use test ad units during development to avoid policy violations:

**AdMob Test Ad Units:**
- Banner: `ca-app-pub-3940256099942544/6300978111`
- Interstitial: `ca-app-pub-3940256099942544/1033173712`
- Rewarded: `ca-app-pub-3940256099942544/5224354917`
- Native: `ca-app-pub-3940256099942544/2247696110`
- App Open: `ca-app-pub-3940256099942544/9257395921`

### 6. Handle Consent Properly
Always gather user consent before showing ads, especially for users in GDPR regions:

```kotlin
lifecycleScope.launch {
    val consentResult = SDKManager.gatherConsent(
        activity = this@SplashActivity,
        debugGeography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
    )

    if (consentResult.isSuccess) {
        // Proceed with loading ads
    }
}
```

### 7. Check Ad Readiness
Always check if an ad is ready before showing it:

```kotlin
if (rewardedAd.isReady()) {
    rewardedAd.show(this, callback)
} else {
    // Load the ad first
    lifecycleScope.launch {
        rewardedAd.load(this@MyActivity)
    }
}
```

### 8. Implement Error Handling
Always implement error callbacks to handle ad failures gracefully:

```kotlin
bannerAd.setListener(object : BannerAdListener() {
    override fun onAdFailedToLoad(error: String) {
        Log.e("Banner", "Failed to load: $error")
        // Hide ad container or show alternative content
    }
})
```

---

## Troubleshooting

### Common Issues

#### 1. AppsFlyer Backup Rules Conflicts

The SDK includes AppsFlyer, which has backup rules in its AndroidManifest.xml to opt out of backing up Shared Preferences data. This prevents retaining the same counters and AppsFlyer ID during app reinstallation, ensuring accurate detection of new installs or re-installs.

You may encounter manifest merger conflicts when integrating the SDK. Here's how to resolve them:

##### Fix conflict with `fullBackupContent="true"`

If you add `android:fullBackupContent="true"` in your AndroidManifest.xml, you might get this error:

```
Manifest merger failed : Attribute application@fullBackupContent value=(true)
```

**Solution**: Add `tools:replace="android:fullBackupContent"` in the `<application>` tag:

```xml
<application
    android:fullBackupContent="true"
    tools:replace="android:fullBackupContent"
    ...>
```

##### Fix conflict with `dataExtractionRules="true"`

If you add `android:dataExtractionRules="true"` in your AndroidManifest.xml, you might get this error:

```
Manifest merger failed : Attribute application@dataExtractionRules value=(true)
```

**Solution**: Add `tools:replace="android:dataExtractionRules"` in the `<application>` tag:

```xml
<application
    android:dataExtractionRules="true"
    tools:replace="android:dataExtractionRules"
    ...>
```

##### Fix conflict with `allowBackup="false"`

If you add `android:allowBackup="false"` in your AndroidManifest.xml, you might get this error:

```
Error:
    Attribute application@allowBackup value=(false) from AndroidManifest.xml:
    is also present at [com.appsflyer:af-android-sdk:6.14.0] AndroidManifest.xml: value=(true).
    Suggestion: add 'tools:replace="android:allowBackup"' to <application> element at AndroidManifest.xml to override.
```

**Solution**: Add `tools:replace="android:allowBackup"` in the `<application>` tag:

```xml
<application
    android:allowBackup="false"
    tools:replace="android:allowBackup"
    ...>
```

##### Merge backup rules for Android 12 and above

If you're targeting Android 12+ and have your own backup rules specified (`android:dataExtractionRules="@xml/my_rules"`), you need to manually merge your backup rules with AppsFlyer's rules.

**Step 1**: Add the `tools:replace` attribute as shown above.

**Step 2**: Create or update your data extraction rules file (e.g., `res/xml/my_rules.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <!-- Your custom rules here -->
        
        <!-- AppsFlyer exclusion rule -->
        <exclude domain="sharedpref" path="appsflyer-data"/>
    </cloud-backup>
    <device-transfer>
        <!-- Your custom rules here -->
        
        <!-- AppsFlyer exclusion rule -->
        <exclude domain="sharedpref" path="appsflyer-data"/>
    </device-transfer>
</data-extraction-rules>
```

**Step 3**: Reference it in your AndroidManifest.xml:

```xml
<application
    android:dataExtractionRules="@xml/my_rules"
    tools:replace="android:dataExtractionRules"
    ...>
```

##### Merge backup rules for Android 11 and below

If you're also targeting Android 11 and lower with your own backup rules (`android:fullBackupContent="@xml/my_backup_rules"`), merge them with AppsFlyer's rules.

**Step 1**: Add the `tools:replace` attribute as shown above.

**Step 2**: Create or update your full backup content file (e.g., `res/xml/my_backup_rules.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Your custom rules here -->
    
    <!-- AppsFlyer exclusion rule -->
    <exclude domain="sharedpref" path="appsflyer-data"/>
</full-backup-content>
```

**Step 3**: Reference it in your AndroidManifest.xml:

```xml
<application
    android:fullBackupContent="@xml/my_backup_rules"
    tools:replace="android:fullBackupContent"
    ...>
```

##### Complete AndroidManifest.xml Example

Here's a complete example with all backup rules properly configured:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".MyApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        tools:replace="android:allowBackup,android:dataExtractionRules,android:fullBackupContent"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MyApp">
        
        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
        
        <!-- Your activities here -->
        
    </application>
</manifest>
```

**Note**: Make sure to add the `tools` namespace at the top of your manifest file:
```xml
xmlns:tools="http://schemas.android.com/tools"
```

---

#### 2. SDK Not Initialized
**Error**: Ads not loading or crashes when calling ad methods

**Solution**: Ensure `SDKManager.initialize()` is called in your Application class before loading any ads.

#### 3. No Ads Loading
**Possible causes**:
- Invalid ad unit IDs
- Network connectivity issues
- Ad inventory not available
- Consent not gathered

**Solution**:
- Verify ad unit IDs are correct
- Check internet connection
- Use test ad units during development
- Ensure consent is gathered before loading ads

#### 4. Remote Config Not Updating
**Error**: Ad configuration not changing

**Solution**:
- Check Firebase Remote Config fetch interval (default 1 hour)
- Call `fetchAndActivate()` to force update
- Verify Remote Config keys match your code

#### 5. Native Ads Not Displaying
**Possible causes**:
- Missing required view IDs in layout
- Layout not properly configured

**Solution**:
- Ensure your native ad layout includes all required view IDs
- Use `NativeAdView` as the root element
- Check logs for binding errors

#### 6. App Open Ads Not Showing
**Possible causes**:
- Ad not pre-loaded
- Showing too frequently
- Activity lifecycle issues

**Solution**:
- Pre-load app open ads in Application class
- Implement proper timing between ad shows
- Check activity lifecycle callbacks

### Getting Test Device ID

To test consent forms and ads, you need your test device ID:

```kotlin
// Add this temporarily to get your device ID
Log.d("TEST_DEVICE", "Device ID: ${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}")
```

Or check logcat for UMP SDK messages that include your hashed device ID.

---

## API Reference

### SDKManager

Main entry point for SDK operations.

#### Methods

- `suspend fun initialize(context: Context, maxSdkKey: String?): Result<Unit>`
    - Initialize the SDK with AdMob and optionally MAX

- `fun updateRemoteConfig(firebaseRemoteConfig: FirebaseRemoteConfig)`
    - Update SDK with new Remote Config values

- `fun addInitializationListener(listener: SDKInitializationListener)`
    - Register a listener for initialization events

- `fun removeInitializationListener(listener: SDKInitializationListener)`
    - Unregister an initialization listener

- `suspend fun gatherConsent(activity: Activity, testDeviceHashedId: String?, debugGeography: Int): Result<Unit>`
    - Gather user consent for ads

- `fun canRequestAds(): Boolean`
    - Check if ads can be requested (consent obtained)

- `var enable: Boolean`
    - Enable or disable all ads globally

### CommonBannerAd

#### Constructor
```kotlin
CommonBannerAd(key: String, defaultUnitId: String)
```

#### Methods
- `fun setListener(listener: BannerAdListener)`
- `suspend fun show(context: Context, container: ViewGroup)`
- `fun destroy()`

### CommonNativeAd

#### Constructor
```kotlin
CommonNativeAd(key: String, defaultUnitId: String, @LayoutRes layoutRes: Int)
```

#### Methods
- `fun setListener(listener: NativeAdListener)`
- `suspend fun load(context: Context)`
- `suspend fun show(context: Context, container: ViewGroup)`
- `fun isReady(): Boolean`

### CommonInterstitialAd

#### Constructor
```kotlin
CommonInterstitialAd(key: String, defaultUnitId: String)
```

#### Methods
- `suspend fun load(context: Context)`
- `fun show(activity: Activity, callback: InterstitialAdShowCallback)`
- `fun isReady(): Boolean`

### CommonRewardedAd

#### Constructor
```kotlin
CommonRewardedAd(key: String, defaultUnitId: String)
```

#### Methods
- `suspend fun load(context: Context)`
- `fun show(activity: Activity, timeoutMs: Long, callback: RewardedAdShowCallback)`
- `fun isReady(): Boolean`

### CommonAppOpenAd

#### Constructor
```kotlin
CommonAppOpenAd(key: String, defaultUnitId: String)
```

#### Methods
- `fun load(context: Context, listener: AdLoadListener)`
- `fun show(activity: Activity, listener: FullScreenAdListener)`
- `fun isReady(): Boolean`
- `fun destroy()`

---

## License

This SDK is proprietary software. Please contact the development team for licensing information.

## Support

For support and questions, please contact:
- **Email**: thuando.work@gmail.com
- **Sample code**: Will be updated soon!
---

## Changelog

### Version 1.0.0-alpha17-SNAPSHOT
- Initial release
- Support for AdMob and AppLovin MAX
- Banner, Native, Interstitial, Rewarded, and App Open ads
- Firebase Remote Config integration
- GDPR/CCPA consent management
- Common API for simplified ad implementation
- Support Singular, Appsflyer for MMP

