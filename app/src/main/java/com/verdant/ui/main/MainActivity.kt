package com.verdant.ui.main

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.verdant.R
import com.verdant.core.auth.AuthManager
import com.verdant.core.navigation.ProtectedNav

class MainActivity : AppCompatActivity() {

    private lateinit var navHome: LinearLayout
    private lateinit var navExplore: LinearLayout
    private lateinit var navFab: FrameLayout
    private lateinit var navMessages: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var navIconHome: ImageView
    private lateinit var navIconExplore: ImageView
    private lateinit var navIconMessages: ImageView
    private lateinit var navIconProfile: ImageView

    private lateinit var customNavBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Bind views
        customNavBar   = findViewById(R.id.navBarBg)
        navHome        = findViewById(R.id.navHome)
        navExplore     = findViewById(R.id.navExplore)
        navFab         = findViewById(R.id.navFab)
        navMessages    = findViewById(R.id.navMessages)
        navProfile     = findViewById(R.id.navProfile)
        navIconHome    = findViewById(R.id.navIconHome)
        navIconExplore = findViewById(R.id.navIconExplore)
        navIconMessages= findViewById(R.id.navIconMessages)
        navIconProfile = findViewById(R.id.navIconProfile)

        val protectedTabs = setOf(
            R.id.messagesFragment,
            R.id.hikeFragment,
            R.id.profileFragment
        )

        // Reusable nav options — single top to avoid duplicate back stack entries
        fun navOptions(destId: Int) = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
            .build()

        fun navigateTo(destId: Int) {
            if (destId in protectedTabs && !AuthManager.isAuthenticated()) {

                ProtectedNav.navigate(
                    navController = navController,
                    destId = destId,
                    args = null,
                    navOptions = null,
                    isProtected = false
                )

                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                if (currentFragment != null) {
                    com.verdant.core.ui.GuestPromptDialog.show(currentFragment.childFragmentManager)
                } else {
                    ProtectedNav.navigate(
                        navController = navController,
                        destId = destId,
                        args = null,
                        navOptions = null,
                        isProtected = false
                    )
                }

            } else {
                navController.navigate(destId, null, navOptions(destId))
            }
        }

        navHome.setOnClickListener    { navigateTo(R.id.homeFragment) }
        navExplore.setOnClickListener { navigateTo(R.id.exploreFragment) }
        navFab.setOnClickListener     { navigateTo(R.id.hikeFragment) }
        navMessages.setOnClickListener{ navigateTo(R.id.messagesFragment) }
        navProfile.setOnClickListener { navigateTo(R.id.profileFragment) }

        // Update active icon tints when destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val authScreens = setOf(
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.splashFragment,
                R.id.onboardingFragment
            )


            customNavBar.visibility =
                if (destination.id in authScreens) View.GONE else View.VISIBLE



            val showNav = destination.id !in authScreens
            customNavBar.visibility = if (showNav) View.VISIBLE else View.GONE
            navFab.visibility       = if (showNav) View.VISIBLE else View.GONE


            // Reset all to inactive
            val inactive = 0xFFAAAAAA.toInt()
            val active   = 0xFF02D083.toInt()

            navIconHome.setColorFilter(inactive)
            navIconExplore.setColorFilter(inactive)
            navIconMessages.setColorFilter(inactive)
            navIconProfile.setColorFilter(inactive)

            // Highlight active tab
            when (destination.id) {
                R.id.homeFragment     -> navIconHome.setColorFilter(active)
                R.id.exploreFragment  -> navIconExplore.setColorFilter(active)
                R.id.messagesFragment -> navIconMessages.setColorFilter(active)
                R.id.profileFragment  -> navIconProfile.setColorFilter(active)
            }
        }
    }
}
