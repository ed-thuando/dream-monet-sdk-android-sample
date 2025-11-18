## Changelog 1.0.0-alpha32-SNAPSHOT

### Key Changes

- Fix issue show loading blink blink.
- Support show loading for reward ads.
```
        rewardedAd.show(
            activity = requireActivity(),
            timeoutMs = 5000, //default 0ms -> no loading. Only show loading if timeOut > 0
            callback = object : RewardedAdShowCallback {
                override fun onShowFailed(error: String) {
                    updateStatus("AdMob rewarded ad failed to show: $error")
                }

                override fun onAdRewarded(isRewardEarned: Boolean) {
                    Toast.makeText(requireContext(), "admob rewarded $isRewardEarned", Toast.LENGTH_SHORT).show()
                    updateStatus("AdMob rewarded ad rewarded: $isRewardEarned")
                }
            }
        )
```