package com.airbnb.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.core.ui.GuestPromptDialog
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navExplore: LinearLayout
    private lateinit var navWishlists: LinearLayout
    private lateinit var navTrips: LinearLayout
    private lateinit var navMessages: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var navIconExplore: ImageView
    private lateinit var navIconWishlists: ImageView
    private lateinit var navIconTrips: ImageView
    private lateinit var navIconMessages: ImageView
    private lateinit var navIconProfile: ImageView

    private lateinit var navLabelExplore: TextView
    private lateinit var navLabelWishlists: TextView
    private lateinit var navLabelTrips: TextView
    private lateinit var navLabelMessages: TextView
    private lateinit var navLabelProfile: TextView

    private lateinit var navProfileInit: TextView
    private lateinit var customNavBar: LinearLayout

    private var lastNavTime = 0L

    companion object {
        private const val NAV_DEBOUNCE_MS = 400L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.nav_host_fragment
            ) as? NavHostFragment ?: return

        val navController = navHostFragment.navController

        // NAV ROOT
        customNavBar = findViewById(R.id.navBarBg)

        // NAV ITEMS
        navExplore = findViewById(R.id.navExplore)
        navWishlists = findViewById(R.id.navWishlists)
        navTrips = findViewById(R.id.navTrips)
        navMessages = findViewById(R.id.navMessages)
        navProfile = findViewById(R.id.navProfile)

        // ICONS
        navIconExplore = findViewById(R.id.navIconExplore)
        navIconWishlists = findViewById(R.id.navIconWishlists)
        navIconTrips = findViewById(R.id.navIconTrips)
        navIconMessages = findViewById(R.id.navIconMessages)
        navIconProfile = findViewById(R.id.navIconProfile)

        // LABELS
        navLabelExplore = findViewById(R.id.navLabelExplore)
        navLabelWishlists = findViewById(R.id.navLabelWishlists)
        navLabelTrips = findViewById(R.id.navLabelTrips)
        navLabelMessages = findViewById(R.id.navLabelMessages)
        navLabelProfile = findViewById(R.id.navLabelProfile)

        navProfileInit = findViewById(R.id.navProfileInitial)

        observeBottomAvatar()

        val protectedTabs = setOf(
            R.id.wishlistFragment,
            R.id.tripsFragment,
            R.id.messagesFragment,
            R.id.profileFragment
        )

        fun navOptions() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                navController.graph.startDestinationId,
                false,
                true
            )
            .build()

        fun navigateTo(destId: Int) {

            val now = System.currentTimeMillis()

            if (now - lastNavTime < NAV_DEBOUNCE_MS) return

            lastNavTime = now

            if (navController.currentDestination?.id == destId) return

            if (destId in protectedTabs &&
                !AuthManager.isAuthenticated()
            ) {

                val currentFragment =
                    navHostFragment.childFragmentManager
                        .primaryNavigationFragment

                if (currentFragment != null) {

                    GuestPromptDialog.show(
                        currentFragment.childFragmentManager
                    )

                } else {

                    navController.navigate(R.id.loginFragment)
                }

                return
            }

            navController.navigate(
                destId,
                null,
                navOptions()
            )
        }

        // CLICK EVENTS
        navExplore.setOnClickListener {
            navigateTo(R.id.exploreFragment)
        }

        navWishlists.setOnClickListener {
            navigateTo(R.id.wishlistFragment)
        }

        navTrips.setOnClickListener {
            navigateTo(R.id.tripsFragment)
        }

        navMessages.setOnClickListener {
            navigateTo(R.id.messagesFragment)
        }

        navProfile.setOnClickListener {
            navigateTo(R.id.profileFragment)
        }

        // DESTINATION LISTENER
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val hiddenScreens = setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.onboardingFragment
            )

            customNavBar.visibility =
                if (destination.id in hiddenScreens)
                    View.GONE
                else
                    View.VISIBLE

            val active =
                ContextCompat.getColor(this, R.color.airbnb_red)

            val inactive =
                ContextCompat.getColor(this, R.color.airbnb_gray)

            // RESET ICONS
            navIconExplore.setColorFilter(inactive)
            navIconWishlists.setColorFilter(inactive)
            navIconTrips.setColorFilter(inactive)
            navIconMessages.setColorFilter(inactive)

            // RESET LABELS
            navLabelExplore.setTextColor(inactive)
            navLabelWishlists.setTextColor(inactive)
            navLabelTrips.setTextColor(inactive)
            navLabelMessages.setTextColor(inactive)
            navLabelProfile.setTextColor(inactive)

            navProfile.alpha =
                if (destination.id == R.id.profileFragment)
                    1f
                else
                    0.7f

            when (destination.id) {

                R.id.exploreFragment -> {
                    navIconExplore.setColorFilter(active)
                    navLabelExplore.setTextColor(active)
                }

                R.id.wishlistFragment -> {
                    navIconWishlists.setColorFilter(active)
                    navLabelWishlists.setTextColor(active)
                }

                R.id.tripsFragment -> {
                    navIconTrips.setColorFilter(active)
                    navLabelTrips.setTextColor(active)
                }

                R.id.messagesFragment -> {
                    navIconMessages.setColorFilter(active)
                    navLabelMessages.setTextColor(active)
                }

                R.id.profileFragment -> {
                    navLabelProfile.setTextColor(active)
                }
            }
        }
    }

    private fun observeBottomAvatar() {

        lifecycleScope.launch {

            AuthManager.currentUserFlow().collect { user ->

                if (user != null) {

                    AvatarHelper.bind(
                        imgView = navIconProfile,
                        tvInitial = navProfileInit,
                        name = user.name.ifBlank {
                            user.email
                        },
                        imageUrl = user.profileImage
                    )

                } else {

                    navIconProfile.setImageResource(
                        R.drawable.ic_profile
                    )

                    navProfileInit.visibility = View.GONE
                }
            }
        }
    }
}