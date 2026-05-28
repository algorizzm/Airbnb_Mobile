package com.airbnb.core.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.airbnb.core.auth.AuthManager
import com.airbnb.ui.auth.GuestPromptDialog

object ProtectedNav {

    /**
     * If guest, show the guest prompt dialog.
     * Returns true if navigation happened.
     */
    fun navigate(
        navController: NavController,
        destId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null,
        isProtected: Boolean = true,
        fragmentManager: FragmentManager? = null,
    ): Boolean {

        if (isProtected && !AuthManager.isAuthenticated()) {

            fragmentManager?.let {
                GuestPromptDialog.show(
                    fragmentManager = it,
                    destId = destId,
                    destArgs = args
                )
            }

            return false
        }

        navController.navigate(destId, args, navOptions)
        return true
    }
}