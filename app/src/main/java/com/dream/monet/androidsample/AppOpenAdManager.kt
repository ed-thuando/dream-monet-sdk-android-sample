package com.dream.monet.androidsample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.dream.monet.ads.api.common.CommonAppOpenAd
import com.dream.monet.ads.listeners.adtype.FullScreenAdListener
import com.dream.monet.ads.listeners.base.AdLoadListener
import com.dream.monet.androidsample.ui.splash.SplashActivity
import com.google.android.gms.ads.AdActivity

class AppOpenAdManager(private val application: Application) : LifecycleObserver {

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921" // Test ad unit
    }

    private val ignoredActivity = setOf(
        SplashActivity::class.java,
        AdActivity::class.java,
    )

    private var appOpenAd: CommonAppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (activity.javaClass in ignoredActivity) return
                currentActivity = activity
                showAdIfAvailable()
            }
            override fun onActivityResumed(activity: Activity) {
                if (activity.javaClass in ignoredActivity) return
                currentActivity = activity
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
                        Log.d("AppOpenAd", "Ad loaded successfully")
                        isLoadingAd = false
                    }

                    override fun onAdLoadFailed(adId: String, error: String, code: Int?, adNetworkType: String?) {
                        Log.e("AppOpenAd", "Ad load failed: $error")
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
                    Log.d("AppOpenAd", "Ad dismissed")
                    isShowingAd = false
                    appOpenAd = null
                    loadAd() // Pre-load the next ad
                }

                override fun onAdFailedToShow(adId: String, error: String, adNetworkType: String?) {
                    Log.e("AppOpenAd", "Ad failed to show: $error")
                    isShowingAd = false
                    appOpenAd = null
                    loadAd()
                }

                override fun onAdImpression(adId: String, adNetworkType: String) {
                    super.onAdImpression(adId, adNetworkType)
                    Log.d("AppOpenAd", "Ad showed")
                }
            }
        )
    }

    fun showAdManually() {
        showAdIfAvailable()
    }
}
