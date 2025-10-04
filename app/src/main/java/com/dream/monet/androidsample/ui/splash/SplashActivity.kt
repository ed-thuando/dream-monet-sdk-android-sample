package com.dream.monet.androidsample.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dream.monet.ads.manager.SDKInitializationListener
import com.dream.monet.ads.manager.SDKManager
import com.dream.monet.androidsample.ui.home.MainActivity
import com.example.dreammonetsample.R
import com.example.dreammonetsample.databinding.ActivitySplashBinding
import com.google.android.ump.ConsentDebugSettings
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity(), SDKInitializationListener {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register for SDK initialization events
        SDKManager.addInitializationListener(this)
    }

    override fun onInitializationComplete(success: Boolean, errorMessage: String?) {
        if (success) {
            Log.d("Splash", "SDK initialized successfully")
        } else {
            Log.e("Splash", "SDK initialization failed: $errorMessage")
            // Even if SDK fails, we might want to proceed to the main app
            // depending on the desired behavior.
        }
    }

    override fun onUpdatedFirebaseRemoteConfig() {
        // Called when Remote Config is fetched and updated
        Log.d("Splash", "Remote Config updated")
        binding.statusText.text = getString(R.string.gathering_consent)

        // Now gather consent
        startConsentGathering()
    }

    private fun startConsentGathering() {
        lifecycleScope.launch {
            val consentResult = SDKManager.gatherConsent(
                activity = this@SplashActivity,
                testDeviceHashedId = null, // Set your test device ID here for testing
                debugGeography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA // Optional, for testing
            )

            if (consentResult.isSuccess) {
                Log.d("Splash", "Consent gathered successfully")
            } else {
                Log.e("Splash", "Consent gathering failed")
            }

            // Navigate to main activity regardless of consent status
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