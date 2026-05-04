package com.hikora

import android.app.Application
import com.hikora.data.session.UserSessionManager

class HikoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        UserSessionManager.start()
    }
}
