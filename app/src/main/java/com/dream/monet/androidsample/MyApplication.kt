package com.dream.monet.androidsample

import android.app.Application
import android.util.Log
import com.dream.monet.ads.core.models.InitializeConfig
import com.dream.monet.ads.manager.SDKManager
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase first
        FirebaseApp.initializeApp(this)

        // 1. Initialize Firebase Remote Config
        initializeFirebaseRemoteConfig()

        // 2. Initialize the Ads SDK
        initializeAdSDK()

        // 3. Initialize App Open Ad Manager
        appOpenAdManager = AppOpenAdManager(this)
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
                // TODO - update your own config here
                val initResult = SDKManager.initialize(
                    context = this@MyApplication,
                    config = InitializeConfig.builder()
                        .setMaxSdkKey("Your-max-sdk")
                        .setTestDeviceIds(
                            setOf(
                                "Your-device-id"
                            )
                        )
                        .build()
                )

                if (initResult.isSuccess) {
                    Log.d("MyApp", "SDK initialized successfully")
                    
                    // Load first app open ad
                    appOpenAdManager.loadAd()
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
            SDKManager.updateRemoteConfig(firebaseRemoteConfig)

        } catch (e: Exception) {
            Log.e("MyApp", "Error fetching remote config: ${e.message}", e)
        }
    }
}
