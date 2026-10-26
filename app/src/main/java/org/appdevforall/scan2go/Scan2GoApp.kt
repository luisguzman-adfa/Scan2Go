package org.appdevforall.scan2go

import android.app.Application
import com.google.android.material.color.DynamicColors

/** Applies Material 3 dynamic color on Android 12+; a no-op below. */
class Scan2GoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
