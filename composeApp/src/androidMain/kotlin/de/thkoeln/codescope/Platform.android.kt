package de.thkoeln.codescope

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun getSystemLoad(): Float {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            (usedMemory.toFloat() / maxMemory.toFloat())
        } catch (e: Exception) {
            0f
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()