package de.thkoeln.codescope

import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    
    override fun getSystemLoad(): Float {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val load = osBean.cpuLoad.toFloat()
        return if (load < 0) 0f else load
    }
}

actual fun getPlatform(): Platform = JVMPlatform()