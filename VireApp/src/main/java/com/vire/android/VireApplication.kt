package com.vire.android

import android.app.Application
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp

class VireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            checkGooglePlayServices()
        } catch (e: Exception) {
            Log.e("VireApplication", "Error during app initialization", e)
        }
    }

    private fun checkGooglePlayServices() {
        try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)
            if (resultCode != ConnectionResult.SUCCESS) {
                Log.w("VireApplication", "Google Play Services not available or needs update. Result code: $resultCode")
            }
        } catch (e: SecurityException) {
            Log.e("VireApplication", "SecurityException checking Google Play Services availability", e)
        } catch (e: Exception) {
            Log.e("VireApplication", "Failed to check Google Play Services availability", e)
        }
    }
}
