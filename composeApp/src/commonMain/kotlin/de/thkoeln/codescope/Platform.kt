package de.thkoeln.codescope

interface Platform {
    val name: String
    
    /**
     * Gibt die aktuelle CPU-Last des Systems zurück (0.0 bis 1.0).
     */
    fun getSystemLoad(): Float
}

expect fun getPlatform(): Platform