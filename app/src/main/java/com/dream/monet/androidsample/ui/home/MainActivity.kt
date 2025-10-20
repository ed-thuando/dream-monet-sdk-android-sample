package com.dream.monet.androidsample.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.applovin.mediation.MaxAd
import com.dream.monet.ads.api.listener.AdRevenueListener
import com.dream.monet.ads.api.listener.RevenueListenerRegistry
import com.example.dreammonetsample.R
import com.example.dreammonetsample.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdValue
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_xml, R.id.navigation_compose
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        RevenueListenerRegistry.register(object : AdRevenueListener {
            override fun onMobileGoogleRevenue(adValue: AdValue) {
                Toast.makeText(this@MainActivity, "onMobileGoogleRevenue ${adValue.valueMicros} $adValue", Toast.LENGTH_SHORT).show()
            }
            override fun onMaxRevenue(maxAd: MaxAd) {
                Toast.makeText(this@MainActivity, "onMaxRevenue ${maxAd.revenue} $maxAd", Toast.LENGTH_SHORT).show()
            }
        })
    }
}