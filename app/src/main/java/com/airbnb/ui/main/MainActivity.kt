package com.airbnb.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.mode.AppMode
import com.airbnb.core.mode.AppModeManager
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.ui.auth.GuestPromptDialog
import com.airbnb.utils.BackfillUtility
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // =========================================================
    // TRAVELER NAV VIEWS
    // =========================================================

    private lateinit var navBarTraveler: LinearLayout

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

    // =========================================================
    // HOST NAV VIEWS
    // =========================================================

    private lateinit var navBarHost: LinearLayout

    private lateinit var navToday: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navListings: LinearLayout
    private lateinit var navHostMessages: LinearLayout
    private lateinit var navHostProfile: LinearLayout

    private lateinit var navIconToday: ImageView
    private lateinit var navIconCalendar: ImageView
    private lateinit var navIconListings: ImageView
    private lateinit var navIconHostMessages: ImageView
    private lateinit var navIconHostProfile: ImageView

    private lateinit var navLabelToday: TextView
    private lateinit var navLabelCalendar: TextView
    private lateinit var navLabelListings: TextView
    private lateinit var navLabelHostMessages: TextView
    private lateinit var navLabelHostProfile: TextView

    private lateinit var navHostProfileInit: TextView

    // =========================================================
    // DEBOUNCE
    // =========================================================

    private var lastNavTime = 0L

    companion object {
        private const val NAV_DEBOUNCE_MS = 400L
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AppModeManager (safe to call multiple times)
        AppModeManager.init(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return
        val navController = navHostFragment.navController

        val graph = navController.navInflater.inflate(R.navigation.main_graph)

        graph.setStartDestination(
            if (AppModeManager.currentModeSnapshot() == AppMode.HOST) {
                R.id.hostTodayFragment
            } else {
                R.id.exploreFragment
            }
        )

        navController.graph = graph

        // ----- TRAVELER VIEWS -----
        navBarTraveler = findViewById(R.id.navBarBg)

        navExplore = findViewById(R.id.navExplore)
        navWishlists = findViewById(R.id.navWishlists)
        navTrips = findViewById(R.id.navTrips)
        navMessages = findViewById(R.id.navMessages)
        navProfile = findViewById(R.id.navProfile)

        navIconExplore = findViewById(R.id.navIconExplore)
        navIconWishlists = findViewById(R.id.navIconWishlists)
        navIconTrips = findViewById(R.id.navIconTrips)
        navIconMessages = findViewById(R.id.navIconMessages)
        navIconProfile = findViewById(R.id.navIconProfile)

        navLabelExplore = findViewById(R.id.navLabelExplore)
        navLabelWishlists = findViewById(R.id.navLabelWishlists)
        navLabelTrips = findViewById(R.id.navLabelTrips)
        navLabelMessages = findViewById(R.id.navLabelMessages)
        navLabelProfile = findViewById(R.id.navLabelProfile)

        navProfileInit = findViewById(R.id.navProfileInitial)

        // ----- HOST VIEWS -----
        navBarHost = findViewById(R.id.navBarHost)

        navToday = findViewById(R.id.navToday)
        navCalendar = findViewById(R.id.navCalendar)
        navListings = findViewById(R.id.navListings)
        navHostMessages = findViewById(R.id.navHostMessages)
        navHostProfile = findViewById(R.id.navHostProfile)

        navIconToday = findViewById(R.id.navIconToday)
        navIconCalendar = findViewById(R.id.navIconCalendar)
        navIconListings = findViewById(R.id.navIconListings)
        navIconHostMessages = findViewById(R.id.navIconHostMessages)
        navIconHostProfile = findViewById(R.id.navIconHostProfile)

        navLabelToday = findViewById(R.id.navLabelToday)
        navLabelCalendar = findViewById(R.id.navLabelCalendar)
        navLabelListings = findViewById(R.id.navLabelListings)
        navLabelHostMessages = findViewById(R.id.navLabelHostMessages)
        navLabelHostProfile = findViewById(R.id.navLabelHostProfile)

        navHostProfileInit = findViewById(R.id.navHostProfileInitial)

        // ----- NAV OPTIONS FACTORY -----
        fun navOptions() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, false, true)
            .build()

        // ----- DEBOUNCED NAVIGATE -----
        fun navigateTo(destId: Int) {
            val now = System.currentTimeMillis()
            if (now - lastNavTime < NAV_DEBOUNCE_MS) return
            lastNavTime = now

            if (navController.currentDestination?.id == destId) return

            navController.navigate(destId, null, navOptions())
        }

        // =========================================================
        // TRAVELER CLICK HANDLERS
        // =========================================================

        navExplore.setOnClickListener { navigateTo(R.id.exploreFragment) }
        navWishlists.setOnClickListener { navigateTo(R.id.wishlistFragment) }
        navTrips.setOnClickListener { navigateTo(R.id.tripsFragment) }
        navMessages.setOnClickListener { navigateTo(R.id.messagesFragment) }
        navProfile.setOnClickListener { navigateTo(R.id.profileFragment) }

        // =========================================================
        // HOST CLICK HANDLERS
        // =========================================================

        navToday.setOnClickListener { navigateTo(R.id.hostTodayFragment) }
        navCalendar.setOnClickListener { navigateTo(R.id.hostCalendarFragment) }
        navListings.setOnClickListener { navigateTo(R.id.hostListingsFragment) }
        navHostMessages.setOnClickListener { navigateTo(R.id.messagesFragment) }
        navHostProfile.setOnClickListener { navigateTo(R.id.profileFragment) }

        // =========================================================
        // DESTINATION CHANGE LISTENER
        // =========================================================

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val hiddenScreens = setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.signupFragment,
            )

            val isHidden = destination.id in hiddenScreens
            val currentMode = AppModeManager.currentModeSnapshot()

            // Show/hide correct nav bar pair
            if (isHidden) {
                navBarTraveler.visibility = View.GONE
                navBarHost.visibility = View.GONE
            } else {
                when (currentMode) {
                    AppMode.TRAVELER -> {
                        navBarTraveler.visibility = View.VISIBLE
                        navBarHost.visibility = View.GONE
                    }
                    AppMode.HOST -> {
                        navBarTraveler.visibility = View.GONE
                        navBarHost.visibility = View.VISIBLE
                    }
                }
            }

            // Highlight active traveler tab
            highlightTravelerTab(destination.id)

            // Highlight active host tab
            highlightHostTab(destination.id)
        }

        // =========================================================
        // REACT TO MODE CHANGES
        // =========================================================

        // repeatOnLifecycle(STARTED) ensures this collector is cancelled while
        // the Activity is in the background — preventing navigation calls from
        // firing when the NavController's state may be inconsistent.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppModeManager.currentMode.collect { mode ->
                    val currentDest = navController.currentDestination?.id
                    val isHidden = currentDest in setOf(
                        R.id.splashFragment,
                        R.id.loginFragment,
                        R.id.signupFragment,
                    )

                    if (!isHidden) {
                        when (mode) {
                            AppMode.TRAVELER -> {
                                navBarTraveler.visibility = View.VISIBLE
                                navBarHost.visibility = View.GONE
                            }

                            AppMode.HOST -> {
                                navBarTraveler.visibility = View.GONE
                                navBarHost.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        // AVATAR OBSERVER
        // =========================================================

        observeBottomAvatar()

        // =========================================================
        // BACKFILL
        // =========================================================

        lifecycleScope.launch {
            try {
                BackfillUtility.runBackfill(FirebaseFirestore.getInstance())
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Public code backfill failed", e)
            }
        }
    }

    // =========================================================
    // HIGHLIGHT HELPERS
    // =========================================================

    private fun highlightTravelerTab(destId: Int) {
        val active = ContextCompat.getColor(this, R.color.airbnb_red)
        val inactive = ContextCompat.getColor(this, R.color.airbnb_gray)

        navIconExplore.setColorFilter(inactive)
        navIconWishlists.setColorFilter(inactive)
        navIconTrips.setColorFilter(inactive)
        navIconMessages.setColorFilter(inactive)

        navLabelExplore.setTextColor(inactive)
        navLabelWishlists.setTextColor(inactive)
        navLabelTrips.setTextColor(inactive)
        navLabelMessages.setTextColor(inactive)
        navLabelProfile.setTextColor(inactive)

        navProfile.alpha = if (destId == R.id.profileFragment) 1f else 0.7f

        when (destId) {
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

    private fun highlightHostTab(destId: Int) {
        val active = ContextCompat.getColor(this, R.color.airbnb_red)
        val inactive = ContextCompat.getColor(this, R.color.airbnb_gray)

        navIconToday.setColorFilter(inactive)
        navIconCalendar.setColorFilter(inactive)
        navIconListings.setColorFilter(inactive)
        navIconHostMessages.setColorFilter(inactive)

        navLabelToday.setTextColor(inactive)
        navLabelCalendar.setTextColor(inactive)
        navLabelListings.setTextColor(inactive)
        navLabelHostMessages.setTextColor(inactive)
        navLabelHostProfile.setTextColor(inactive)

        navHostProfile.alpha = if (destId == R.id.profileFragment) 1f else 0.7f

        when (destId) {
            R.id.hostTodayFragment -> {
                navIconToday.setColorFilter(active)
                navLabelToday.setTextColor(active)
            }
            R.id.hostCalendarFragment -> {
                navIconCalendar.setColorFilter(active)
                navLabelCalendar.setTextColor(active)
            }
            R.id.hostListingsFragment -> {
                navIconListings.setColorFilter(active)
                navLabelListings.setTextColor(active)
            }
            R.id.messagesFragment -> {
                navIconHostMessages.setColorFilter(active)
                navLabelHostMessages.setTextColor(active)
            }
            R.id.profileFragment -> {
                navLabelHostProfile.setTextColor(active)
            }
        }
    }

    // =========================================================
    // AVATAR OBSERVER
    // =========================================================

    private fun observeBottomAvatar() {
        lifecycleScope.launch {
            AuthManager.currentUserFlow().collect { user ->
                if (user != null) {
                    // Traveler profile avatar
                    AvatarHelper.bind(
                        imgView = navIconProfile,
                        tvInitial = navProfileInit,
                        name = user.name.ifBlank { user.email },
                        imageUrl = user.profileImage
                    )
                    // Host profile avatar (mirrors the traveler one)
                    AvatarHelper.bind(
                        imgView = navIconHostProfile,
                        tvInitial = navHostProfileInit,
                        name = user.name.ifBlank { user.email },
                        imageUrl = user.profileImage
                    )
                } else {
                    navIconProfile.setImageResource(R.drawable.ic_profile)
                    navProfileInit.visibility = View.GONE

                    navIconHostProfile.setImageResource(R.drawable.ic_profile)
                    navHostProfileInit.visibility = View.GONE

                }
            }
        }
    }
}