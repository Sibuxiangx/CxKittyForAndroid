package github.psicodes.ktxpy

import com.google.android.material.color.DynamicColors
import com.termux.shared.termux.shell.TermuxShellManager

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

    }
}