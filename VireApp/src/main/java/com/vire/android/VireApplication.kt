package com.vire.android

import android.app.Application
import com.google.firebase.FirebaseApp

class VireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
