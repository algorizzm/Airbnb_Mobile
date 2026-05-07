package com.verdant.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.verdant.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.verdant.core.auth.AuthManager
import com.verdant.core.navigation.ProtectedNav

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Connect BottomNav
        bottomNav.setupWithNavController(navController)

        // RBAC: block protected tabs for guests.
        // Important: we still delegate actual navigation to NavigationUI to keep BottomNav back stack stable.
        bottomNav.setOnItemSelectedListener { item ->
            val protectedTabs = setOf(
                R.id.messagesFragment,
                R.id.hikeFragment,
                R.id.profileFragment
            )

            if (item.itemId in protectedTabs && !AuthManager.isAuthenticated()) {
                ProtectedNav.navigate(
                    navController = navController,
                    destId = item.itemId,
                    args = null,
                    navOptions = null,
                    isProtected = true
                )
                false
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // 🔐 Hide navbar on auth screens ONLY
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val authScreens = setOf(
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.splashFragment,
                R.id.onboardingFragment
            )

            bottomNav.visibility =
                if (destination.id in authScreens) View.GONE else View.VISIBLE
        }
    }
}