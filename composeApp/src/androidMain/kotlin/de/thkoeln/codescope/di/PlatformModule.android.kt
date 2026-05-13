package de.thkoeln.codescope.di

import de.thkoeln.codescope.data.client.*
import de.thkoeln.codescope.data.repository.ISettingsRepository
import de.thkoeln.codescope.data.repository.SettingsRepositoryImpl
import de.thkoeln.codescope.logic.INotificationService
import de.thkoeln.codescope.logic.NotificationServiceImpl
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.storage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*


actual val platformModule: Module = module {
    // GoogleAuth
    single { GoogleAuth(androidContext()) }

    // Firebase instances (Android only)
    single { Firebase.auth }
    single { Firebase.firestore }
    single { Firebase.functions(region = "europe-west3") }
    single { Firebase.storage }

    // HttpClient für Android hinzufügen
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json()
            }
        }
    }

    // Settings Repository
    single<ISettingsRepository> { SettingsRepositoryImpl(androidContext()) }

    // Notification Service
    single<INotificationService> { NotificationServiceImpl(androidContext()) }

    // Clients
    single<IFirestoreClient> { FirestoreClientImpl(get()) }
    single<IFirebaseStorageClient> { FirebaseStorageClientImpl(get()) }
    single<IRestClient> { RestClientImpl(get()) }

    // Auth Provider (Android Firebase)
    single<IAuthProvider> { AuthProviderImpl(get(), get(), get()) }
}
