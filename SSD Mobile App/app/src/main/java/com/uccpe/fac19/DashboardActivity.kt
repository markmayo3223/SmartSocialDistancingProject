package com.uccpe.fac19

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uccpe.fac19.Constants.ACTION_SHOW_MAPS_FRAGMENT
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_dashboard.*

class DashboardActivity : AppCompatActivity() {
    override fun getSupportFragmentManager(): FragmentManager {
        return super.getSupportFragmentManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        navigateToMapFragmentIfNeeded(intent)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navController = findNavController(R.id.fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment,R.id.mapsFragment,R.id.settingsFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        bottomNavigationView.setupWithNavController(navController)

    }

    private fun navigateToMapFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_MAPS_FRAGMENT) {
            fragment.findNavController().navigate(R.id.action_global_map_fragment)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToMapFragmentIfNeeded(intent)
    }
}