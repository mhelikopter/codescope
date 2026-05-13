package de.thkoeln.codescope

import android.app.Application
import de.thkoeln.codescope.di.initKoin
import org.koin.android.ext.koin.androidContext

class CodeScopeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CodeScopeApplication)
        }
    }
}
