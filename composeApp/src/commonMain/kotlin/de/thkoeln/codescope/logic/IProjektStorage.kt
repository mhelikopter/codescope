package de.thkoeln.codescope.logic.storage

interface IProjektStorage {

    /**
     * Lädt den Quellcode eines Projekts in den persistenten Storage hoch.
     *
     * @param projektId ID des Projekts
     * @param zipDaten  Gezippter Quellcode
     *
     * @return Storage-Pfad bei Erfolg
     */
    suspend fun uploadProjektQuellcode(
        projektId: String,
        zipDaten: ByteArray
    ): Result<String>
}