package com.verdant.core.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.verdant.R
import com.verdant.core.auth.AuthManager
import com.verdant.core.ui.GuestPromptDialog

object ProtectedNav {
    /**
     * If guest, show the guest prompt dialog.
     * Returns true if navigation to [destId] happened, false if guest prompt was shown.
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
            if (fragmentManager != null) {
                GuestPromptDialog.show(fragmentManager)
            } else {
                val loginArgs = Bundle().apply {
                    putInt(AuthNavKeys.POST_LOGIN_DEST_ID, destId)
                    if (args != null) putBundle(AuthNavKeys.POST_LOGIN_ARGS, args)
                }
                navController.navigate(R.id.loginFragment, loginArgs)
            }
            return false
        }

        navController.navigate(destId, args, navOptions)
        return true
    }
}