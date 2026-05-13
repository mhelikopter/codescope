package de.thkoeln.codescope.di

import de.thkoeln.codescope.BuildKonfig
import de.thkoeln.codescope.AppContext
import de.thkoeln.codescope.JVMAppContext
import de.thkoeln.codescope.data.client.*
import de.thkoeln.codescope.data.repository.ISettingsRepository
import de.thkoeln.codescope.data.repository.SettingsRepositoryImpl
import de.thkoeln.codescope.logic.INotificationService
import de.thkoeln.codescope.logic.NotificationServiceImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {

    // AppContext für JVM
    single<AppContext> { JVMAppContext() }

    // Zentrales JSON-Objekt für Serialisierung
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }

    // Ktor HttpClient konfiguriert für JSON-Datenaustausch
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000 // 5 Minuten
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 300_000
            }
        }
    }

    // Settings Repository
    single<ISettingsRepository> { SettingsRepositoryImpl() }

    // Notification Service
    single<INotificationService> { NotificationServiceImpl() }

    // Firebase Session: Lädt beim App-Start die Sitzung von der Festplatte
    single {
        val sessionFile = File(System.getProperty("user.home"), ".codescope_session.json")
        val json = get<Json>()

        if (sessionFile.exists()) {
            try {
                println("JVM Auth: Lade bestehende Sitzung von ${sessionFile.absolutePath}")
                json.decodeFromString<FirebaseSession>(sessionFile.readText())
            } catch (e: Exception) {
                println("JVM Auth: Sitzungsdatei korrupt oder veraltet, erstelle neue Sitzung.")
                FirebaseSession()
            }
        } else {
            println("JVM Auth: Keine Sitzungsdatei gefunden.")
            FirebaseSession()
        }
    }

    // Firebase Konfiguration aus der BuildKonfig (generiert aus local.properties)
    single {
        FirebaseJvmConfig(
            apiKey = BuildKonfig.FIREBASE_API_KEY,
            projectId = BuildKonfig.FIREBASE_PROJECT_ID,
            storageBucket = BuildKonfig.FIREBASE_STORAGE_BUCKET,
            functionsRegion = BuildKonfig.FIREBASE_FUNCTIONS_REGION
        )
    }

    // GoogleAuth für Desktop (Browser-basiert)
    single { GoogleAuth(Unit) }

    // ---- Clients (Manuelle REST Implementierungen für Desktop) ----
    single<IFirestoreClient> { FirestoreClientImpl(get(), get(), get(), get()) }
    single<IFirebaseStorageClient> { FirebaseStorageClientImpl(get(), get(), get()) }
    single<IRestClient> { RestClientImpl(get(), get(), get()) }

    // ---- Auth Provider ----
    single<IAuthProvider> { AuthProviderImpl(get(), get(), get(), get(), get()) }
}
