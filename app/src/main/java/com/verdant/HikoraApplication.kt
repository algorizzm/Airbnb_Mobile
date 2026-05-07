package com.verdant

import android.app.Application
import com.google.firebase.FirebaseApp
import com.verdant.data.session.UserSessionManager

class VerdantApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 🔥 CRITICAL: Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Your session manager
        UserSessionManager.start()
    }
}