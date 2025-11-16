## Changelog 1.0.0-alpha31-SNAPSHOT

### Key Changes

Add LoadingOverlay when show inter ads, use by adding param isShowLoading when CommonInterstitialAd.show() 
```
    interstitialAd.show(
        requireActivity(),
        0,
        isShowLoading = true,
        callback = object : InterstitialAdShowCallback {
            override fun onAdShowed() {
          
            }

            override fun onShowFailed(error: String) {
   
            }
        }
    )
```