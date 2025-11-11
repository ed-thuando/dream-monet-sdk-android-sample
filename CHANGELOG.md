## Changelog 1.0.0-alpha27-SNAPSHOT

### Key Changes

*  Add AdActivities config at SDK Initialize. Please add all Interstitial Ad Activities from Admob mediation Ad networks.
``` kotlin
val initResult = SDKManager.initialize(
          context = this@MyApp,
          config = InitializeConfig.builder()
            .setMaxSdkKey(BuildConfig.KEY_MAX)
            .setAppsFlyerKey(BuildConfig.KEY_APPSFLYER)
            .setAdActivities(
              setOf(
                AdActivity::class.java,
                TTFullScreenExpressVideoActivity::class.java
              )
            )
            .build(),
        )
```
