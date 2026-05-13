package de.thkoeln.codescope.di

import de.thkoeln.codescope.data.repository.AnalyseVerwaltungImpl
import de.thkoeln.codescope.data.repository.BenutzerVerwaltungImpl
import de.thkoeln.codescope.data.repository.IAnalyseVerwaltung
import de.thkoeln.codescope.data.repository.IBenutzerVerwaltung
import de.thkoeln.codescope.data.repository.IKriterienKatalogVerwaltung
import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.data.repository.IProjektVerwaltung
import de.thkoeln.codescope.data.repository.KriterienKatalogVerwaltungImpl
import de.thkoeln.codescope.data.repository.KursVerwaltungImpl
import de.thkoeln.codescope.data.repository.ProjektVerwaltungImpl
import de.thkoeln.codescope.logic.AdminSteuerung
import de.thkoeln.codescope.logic.AnalyseSteuerung
import de.thkoeln.codescope.logic.DozentKursSteuerung
import de.thkoeln.codescope.logic.IAdminSteuerung
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IDozentKursSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.ILoginSteuerung
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import de.thkoeln.codescope.logic.KriterienkatalogSteuerung
import de.thkoeln.codescope.logic.LoginSteuerung
import de.thkoeln.codescope.logic.ProjektSteuerung
import de.thkoeln.codescope.logic.StudentKursSteuerung
import de.thkoeln.codescope.logic.storage.IProjektStorage
import de.thkoeln.codescope.logic.storage.ProjektStorage
import de.thkoeln.codescope.viewmodel.AppViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

// Common Module (Shared logic only – NO Firebase/GitLive here!)
val commonModule = module {

    // Verwaltung classes
    single<IAnalyseVerwaltung> { AnalyseVerwaltungImpl(get()) }
    single<IBenutzerVerwaltung> { BenutzerVerwaltungImpl(get()) }
    single<IKriterienKatalogVerwaltung> { KriterienKatalogVerwaltungImpl(get(), get(), get()) }
    single<IKursVerwaltung> { KursVerwaltungImpl(get()) }
    single<IProjektVerwaltung> { ProjektVerwaltungImpl(get(), get()) }
    single<IProjektStorage> { ProjektStorage(get()) }

    // Steuerung classes
    single<ILoginSteuerung> { LoginSteuerung(get(), get()) }
    single<IAdminSteuerung> { AdminSteuerung(get(), get(), get()) }
    single<IAnalyseSteuerung> { AnalyseSteuerung(get(), get(), get(), get(), get()) }
    single<IProjektSteuerung> { ProjektSteuerung(get(), get()) }
    single<IDozentKursSteuerung> { DozentKursSteuerung(get()) }
    single<IStudentKursSteuerung> { StudentKursSteuerung(get(), get()) }
    single<IKriterienkatalogSteuerung> { KriterienkatalogSteuerung(get(), get(), get()) }

    // ViewModels
    factory { AppViewModel(get(), get(), get()) }
}

// Initialize Koin (Called from Android/Desktop)
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        commonModule,
        platformModule // <-- expect/actual platform bindings (Android/JVM)
    )
}
