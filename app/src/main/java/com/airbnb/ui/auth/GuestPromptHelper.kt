package com.airbnb.ui.auth

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.google.android.material.button.MaterialButton

/**
 * Helper object for displaying guest restriction prompts.
 * Provides a consistent UX for features that require authentication.
 */
object GuestPromptHelper {

    /**
     * Configures a guest prompt layout with custom messaging and navigation.
     *
     * @param promptLayout The root layout containing guest prompt views
     * @param fragment The fragment hosting the prompt (for navigation)
     * @param title Custom title text (default: "Sign in to continue")
     * @param message Custom message text
     * @param iconRes Custom icon resource (default: profile icon)
     */
    fun setupGuestPrompt(
        promptLayout: View,
        fragment: Fragment,
        title: String = fragment.getString(R.string.guest_prompt_title_default),
        message: String = fragment.getString(R.string.guest_prompt_message_default),
        @DrawableRes iconRes: Int = R.drawable.ic_profile
    ) {
        // Find views
        val imgIcon = promptLayout.findViewById<ImageView>(R.id.imgGuestIcon)
        val tvTitle = promptLayout.findViewById<TextView>(R.id.tvGuestTitle)
        val tvMessage = promptLayout.findViewById<TextView>(R.id.tvGuestMessage)
        val btnSignIn = promptLayout.findViewById<MaterialButton>(R.id.btnGuestSignIn)
        val tvSignUp = promptLayout.findViewById<TextView>(R.id.tvGuestSignUp)

        // Set custom content
        imgIcon?.setImageResource(iconRes)
        tvTitle?.text = title
        tvMessage?.text = message

        // Set click listeners
        btnSignIn?.setOnClickListener {
            navigateToAuth(fragment)
        }

        tvSignUp?.setOnClickListener {
            navigateToAuth(fragment)
        }
    }

    /**
     * Shows the guest prompt layout and hides the content layout.
     */
    fun showGuestPrompt(
        promptLayout: View,
        contentLayout: View
    ) {
        promptLayout.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
    }

    /**
     * Hides the guest prompt layout and shows the content layout.
     */
    fun hideGuestPrompt(
        promptLayout: View,
        contentLayout: View
    ) {
        promptLayout.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
    }

    /**
     * Navigates to the authentication flow.
     */
    private fun navigateToAuth(fragment: Fragment) {
        try {
            val navController = fragment.findNavController()
            val authDest = navController.graph.findNode(R.id.auth_graph)
            if (authDest != null) {
                navController.navigate(R.id.auth_graph)
            }
        } catch (e: Exception) {
            // Navigation failed - fragment may be detached
        }
    }
}

/**
 * Extension function to easily check if user is authenticated.
 */
fun Fragment.isUserAuthenticated(): Boolean {
    return com.airbnb.core.auth.AuthManager.isAuthenticated()
}
