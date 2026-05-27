package com.airbnb.ui.main

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.navigation.ProtectedNav
import com.airbnb.core.ui.AvatarHelper
import kotlinx.coroutines.launch

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
    private lateinit var navProfileInit: TextView

    private lateinit var customNavBar: LinearLayout

    // Prevent rapid duplicate navigations
    private var lastNavTime = 0L

    companion object {
        private const val NAV_DEBOUNCE_MS = 400L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // ─────────────────────────────────────────────
        // Bind Views
        // ─────────────────────────────────────────────
        customNavBar = findViewById(R.id.navBarBg)

        navHome = findViewById(R.id.navHome)
        navExplore = findViewById(R.id.navExplore)
        navFab = findViewById(R.id.navFab)
        navMessages = findViewById(R.id.navMessages)
        navProfile = findViewById(R.id.navProfile)
        navProfileInit = findViewById(R.id.navProfileInitial)

        navIconHome = findViewById(R.id.navIconHome)
        navIconExplore = findViewById(R.id.navIconExplore)
        navIconMessages = findViewById(R.id.navIconMessages)
        navIconProfile = findViewById(R.id.navIconProfile)

        // Observe avatar updates
        observeBottomAvatar()

        val protectedTabs = setOf(
            R.id.messagesFragment,
            R.id.exploreFragment,
            R.id.hikeFragment,
            R.id.profileFragment
        )

        // ─────────────────────────────────────────────
        // Navigation Options
        // ─────────────────────────────────────────────
        fun navOptions() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(
                navController.graph.startDestinationId,
                inclusive = false
            )
            .setEnterAnim(R.anim.nav_enter)
            .setExitAnim(R.anim.nav_exit)
            .setPopEnterAnim(R.anim.nav_pop_enter)
            .setPopExitAnim(R.anim.nav_pop_exit)
            .build()

        // ─────────────────────────────────────────────
        // Navigation Helper
        // ─────────────────────────────────────────────
        fun navigateTo(destId: Int) {

            // Prevent rapid duplicate taps
            val now = System.currentTimeMillis()

            if (now - lastNavTime < NAV_DEBOUNCE_MS) {
                return
            }

            lastNavTime = now

            // Prevent navigating to same destination
            if (navController.currentDestination?.id == destId) {
                return
            }

            // Protected tabs
            if (destId in protectedTabs && !AuthManager.isAuthenticated()) {

                val currentFragment =
                    navHostFragment.childFragmentManager.primaryNavigationFragment

                if (currentFragment != null) {

                    com.airbnb.core.ui.GuestPromptDialog.show(
                        currentFragment.childFragmentManager
                    )

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

                navController.navigate(
                    destId,
                    null,
                    navOptions()
                )
            }
        }

        // ─────────────────────────────────────────────
        // Bottom Nav Clicks
        // ─────────────────────────────────────────────
        navHome.setOnClickListener {
            navigateTo(R.id.homeFragment)
        }

        navExplore.setOnClickListener {
            navigateTo(R.id.exploreFragment)
        }

        navFab.setOnClickListener {
            navigateTo(R.id.hikeFragment)
        }

        navMessages.setOnClickListener {
            navigateTo(R.id.messagesFragment)
        }

        navProfile.setOnClickListener {
            navigateTo(R.id.profileFragment)
        }

        // ─────────────────────────────────────────────
        // Destination Changes
        // ─────────────────────────────────────────────
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val authScreens = setOf(
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.settingsFragment,
                R.id.accountInformationFragment,
                R.id.privacySecurityFragment,
                R.id.notificationSettingsFragment,
                R.id.changePasswordFragment,
                R.id.createEditHikeFragment
            )

            val showNav = destination.id !in authScreens

            customNavBar.visibility =
                if (showNav) View.VISIBLE else View.GONE

            navFab.visibility =
                if (showNav) View.VISIBLE else View.GONE

            // Colors
            val inactive = 0xFFAAAAAA.toInt()
            val active = 0xFF02D083.toInt()

            // Reset icon colors
            navIconHome.setColorFilter(inactive)
            navIconExplore.setColorFilter(inactive)
            navIconMessages.setColorFilter(inactive)

            // Do NOT tint profile avatar
            navProfile.alpha = 0.7f

            // Highlight active tab
            when (destination.id) {

                R.id.homeFragment -> {
                    navIconHome.setColorFilter(active)
                }

                R.id.exploreFragment -> {
                    navIconExplore.setColorFilter(active)
                }

                R.id.messagesFragment -> {
                    navIconMessages.setColorFilter(active)
                }

                R.id.profileFragment -> {
                    navProfile.alpha = 1f
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Observe User Avatar
    // ─────────────────────────────────────────────
    private fun observeBottomAvatar() {

        lifecycleScope.launch {

            AuthManager.currentUserFlow().collect { user ->

                if (user != null) {

                    AvatarHelper.bind(
                        imgView = navIconProfile,
                        tvInitial = navProfileInit,
                        name = user.name.ifBlank { user.email },
                        imageUrl = user.profileImage
                    )

                } else {

                    navIconProfile.setImageResource(R.drawable.ic_profile)
                }
            }
        }
    }
}