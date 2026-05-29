package com.airbnb

import android.app.Application
import com.airbnb.core.mode.AppModeManager
import com.airbnb.data.session.UserSessionManager
import com.airbnb.utils.cloudinary.CloudinaryManager
import com.google.firebase.FirebaseApp

class AirbnbApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase first
        FirebaseApp.initializeApp(this)

        // Initialize Cloudinary SDK (reads credentials from BuildConfig)
        CloudinaryManager.init(this)

        // IMPORTANT:
        // Initialize AppModeManager BEFORE auth/session listeners
        AppModeManager.init(this)

        // Start session manager AFTER everything is ready
        UserSessionManager.start()
    }
}