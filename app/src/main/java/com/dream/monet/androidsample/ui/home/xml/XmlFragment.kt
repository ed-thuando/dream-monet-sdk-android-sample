package com.dream.monet.androidsample.ui.home.xml

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dream.monet.androidsample.AppOpenAdManager
import com.example.dreammonetsample.R
import com.example.dreammonetsample.databinding.FragmentXmlBinding
import com.dream.monet.ads.api.common.CommonBannerAd
import com.dream.monet.ads.api.common.CommonInterstitialAd
import com.dream.monet.ads.api.common.CommonNativeAd
import com.dream.monet.ads.api.common.CommonRewardedAd
import com.dream.monet.ads.listeners.adtype.BannerAdListener
import com.dream.monet.ads.listeners.adtype.NativeAdListener
import com.dream.monet.ads.listeners.adtype.RewardedAdShowCallback
import kotlinx.coroutines.launch

class XmlFragment : Fragment() {

    private var _binding: FragmentXmlBinding? = null
    private val binding get() = _binding!!

    private lateinit var bannerAd: CommonBannerAd
    private lateinit var nativeAd: CommonNativeAd
    private lateinit var interstitialAd: CommonInterstitialAd
    private lateinit var rewardedAd: CommonRewardedAd
    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentXmlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAds()
        setupClickListeners()

        loadAds()
    }

    private fun setupAds() {
        appOpenAdManager = AppOpenAdManager(requireActivity().application)

        // Banner Ad
        bannerAd = CommonBannerAd(
            key = "my_banner_key",
            defaultUnitId = "ca-app-pub-3940256099942544/6300978111"
        )
        bannerAd.setListener(object : BannerAdListener() {
            override fun onAdLoaded(adId: String, adNetworkType: String) {
                super.onAdLoaded(adId, adNetworkType)
                Log.d("XmlFragment", "Banner ad loaded")
            }

            override fun onAdLoadFailed(
                adId: String,
                error: String,
                code: Int?,
                adNetworkType: String?
            ) {
                super.onAdLoadFailed(adId, error, code, adNetworkType)
                Log.e("XmlFragment", "Banner ad failed to load: $error")
            }
        })

        // Native Ad
        nativeAd = CommonNativeAd(
            key = "my_native_key",
            defaultUnitId = "ca-app-pub-3940256099942544/2247696110",
            layoutRes = R.layout.ad_unified
        )
        nativeAd.setListener(object : NativeAdListener() {
            override fun onAdLoaded(adId: String, adNetworkType: String) {
                super.onAdLoaded(adId, adNetworkType)
                Log.d("XmlFragment", "Native ad loaded")
                showNativeAd()
            }

            override fun onAdLoadFailed(
                adId: String,
                error: String,
                code: Int?,
                adNetworkType: String?
            ) {
                super.onAdLoadFailed(adId, error, code, adNetworkType)
                Log.e("XmlFragment", "Native ad failed to load: $error")
            }
        })

        // Interstitial Ad
        interstitialAd = CommonInterstitialAd(
            key = "my_interstitial_key",
            defaultUnitId = "ca-app-pub-3940256099942544/1033173712"
        )

        // Rewarded Ad
        rewardedAd = CommonRewardedAd(
            key = "my_rewarded_key",
            defaultUnitId = "ca-app-pub-3940256099942544/5224354917"
        )
    }

    private fun setupClickListeners() {
        binding.showInterstitialButton.setOnClickListener {
            showInterstitialAd()
        }

        binding.showRewardedButton.setOnClickListener {
            showRewardedAd()
        }

        binding.showAppOpenButton.setOnClickListener {
            appOpenAdManager.showAdManually()
        }
    }

    private fun loadAds() {
        lifecycleScope.launch {
            // Show Banner Ad
            bannerAd.show(requireContext(), binding.bannerAdContainer)

            // Load other ads
            nativeAd.load(requireContext())
            interstitialAd.load(requireContext())
            rewardedAd.load(requireContext())
        }
    }

    private fun showNativeAd() {
        lifecycleScope.launch {
            if (nativeAd.isReady()) {
                nativeAd.show(requireContext(), binding.nativeAdContainer)
            } else {
                Log.d("XmlFragment", "Native ad not ready to show")
            }
        }
    }

    private fun showInterstitialAd() {
        if (interstitialAd.isReady()) {
            interstitialAd.show(requireActivity()) {
                Log.d("XmlFragment", "Interstitial ad show completed")
                Toast.makeText(requireContext(), "Interstitial ad show completed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("XmlFragment", "Interstitial ad not ready yet")
            Toast.makeText(requireContext(), "Interstitial ad not ready", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRewardedAd() {
        if (rewardedAd.isReady()) {
            rewardedAd.show(
                activity = requireActivity(),
                callback = object : RewardedAdShowCallback {
                    override fun onAdRewarded(isRewardEarned: Boolean) {
                        if (isRewardEarned) {
                            Log.d("XmlFragment", "User earned reward!")
                            Toast.makeText(requireContext(), "You earned 100 coins!", Toast.LENGTH_SHORT).show()
                        }
                        lifecycleScope.launch {
                            rewardedAd.load(requireContext())
                        }
                    }

                    override fun onShowFailed(error: String) {
                        Log.e("XmlFragment", "Rewarded ad show failed: $error")
                    }
                }
            )
        } else {
            Log.d("XmlFragment", "Rewarded ad not ready yet")
            Toast.makeText(requireContext(), "Rewarded ad not ready", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        bannerAd.pause()
    }

    override fun onResume() {
        super.onResume()
        bannerAd.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerAd.destroy()
        nativeAd.destroy()
        _binding = null
    }
}
