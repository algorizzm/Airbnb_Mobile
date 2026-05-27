package com.airbnb.core.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.airbnb.R

/** Default animated NavOptions used for every screen transition. */
fun animNavOptions() = NavOptions.Builder()
    .setEnterAnim(R.anim.nav_enter)
    .setExitAnim(R.anim.nav_exit)
    .setPopEnterAnim(R.anim.nav_pop_enter)
    .setPopExitAnim(R.anim.nav_pop_exit)
    .build()

/**
 * Navigate with smooth animations applied automatically.
 * Use this instead of findNavController().navigate() in all fragments.
 */
fun Fragment.navigateTo(destId: Int, args: Bundle? = null) {
    try {
        findNavController().navigate(destId, args, animNavOptions())
    } catch (_: Exception) { /* already navigating */ }
}

fun NavController.navigateAnimated(destId: Int, args: Bundle? = null) {
    try {
        navigate(destId, args, animNavOptions())
    } catch (_: Exception) { /* already navigating */ }
}