package com.verdant.core.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.verdant.R
import com.verdant.core.auth.AuthManager

object ProtectedNav {
    /**
     * If guest, redirect to login and preserve intended destination.
     * Returns true if navigation to [destId] happened, false if login redirect happened.
     */
    fun navigate(
        navController: NavController,
        destId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null,
        isProtected: Boolean = true,
    ): Boolean {
        if (isProtected && !AuthManager.isAuthenticated()) {
            val loginArgs = Bundle().apply {
                putInt(AuthNavKeys.POST_LOGIN_DEST_ID, destId)
                if (args != null) putBundle(AuthNavKeys.POST_LOGIN_ARGS, args)
            }
            navController.navigate(R.id.loginFragment, loginArgs)
            return false
        }

        navController.navigate(destId, args, navOptions)
        return true
    }
}

