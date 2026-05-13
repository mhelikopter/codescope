import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("com.codingfeline.buildkonfig") version "0.17.1"
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.0.20"
    alias(libs.plugins.dokka)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)

            // Auth & Google
            implementation(libs.play.services.auth)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            // Firebase Android SDK (Native)
            implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
            implementation("com.google.firebase:firebase-auth")
            implementation("com.google.firebase:firebase-firestore")
            implementation("com.google.firebase:firebase-storage")
            implementation("com.google.firebase:firebase-functions")

            // Ktor Android Engine
            implementation("io.ktor:ktor-client-android:2.3.12")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.precompose)
            implementation(libs.precompose.koin)

            // Ktor & Serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Gitlive Firebase (Multiplatform)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.functions)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // String-Notation verwenden, da der libs-Alias fehlt
            implementation("io.ktor:ktor-client-cio:2.3.12")
            // SLF4J Logger-Implementierung hinzufügen
            implementation("org.slf4j:slf4j-simple:2.0.9")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "de.thkoeln.codescope"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.thkoeln.codescope"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "de.thkoeln.codescope.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CodeScope"
            packageVersion = "4.2.0"
            vendor = "TH Koeln"
            copyright = "© 2026 TH Koeln"

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.icns"))
                bundleID = "de.thkoeln.codescope"
            }

            windows {
                shortcut = true
                menu = true
                val icoFile = project.file("src/jvmMain/resources/icons/icon.ico")
                if (icoFile.exists()) {
                    iconFile.set(icoFile)
                } else {
                    iconFile.set(project.file("src/jvmMain/resources/icons/icon.png"))
                }
            }
        }
    }
}

buildkonfig {
    packageName = "de.thkoeln.codescope"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    fun getProp(vararg keys: String): String {
        for (key in keys) {
            val value = localProperties.getProperty(key)
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "DESKTOP_CLIENT_ID", getProp("CODE_SCOPE_DESKTOP_CLIENT_ID", "DESKTOP_CLIENT_ID"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "DESKTOP_CLIENT_SECRET", getProp("CODE_SCOPE_DESKTOP_CLIENT_SECRET", "DESKTOP_CLIENT_SECRET"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "FIREBASE_API_KEY", getProp("CODE_SCOPE_FIREBASE_API_KEY", "FIREBASE_API_KEY", "FIREBASE_API_KEY"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "FIREBASE_PROJECT_ID", getProp("CODE_SCOPE_FIREBASE_PROJECT_ID", "FIREBASE_PROJECT_ID", "FIREBASE_PROJECT_ID"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "FIREBASE_STORAGE_BUCKET", getProp("CODE_SCOPE_FIREBASE_STORAGE_BUCKET", "FIREBASE_STORAGE_BUCKET", "FIREBASE_STORAGE_BUCKET"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "FIREBASE_FUNCTIONS_REGION", getProp("FIREBASE_FUNCTIONS_REGION").ifBlank { "europe-west3" })
    }
}

// Dokka output and source-set configuration for multiplatform docs.
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    enabled = true
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

    // Only document main source sets, skip test source sets
    dokkaSourceSets.configureEach {
        suppress.set(name.contains("test", ignoreCase = true))
    }
}