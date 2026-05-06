package com.hikora

import android.app.Application
import com.google.firebase.FirebaseApp
import com.hikora.data.session.UserSessionManager

class HikoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 🔥 CRITICAL: Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Your session manager
        UserSessionManager.start()
    }
}