# CodeScope

CodeScope is a Kotlin Multiplatform desktop and Android application that helps university courses automate code review. Lecturers define **criteria catalogs** (sets of grading questions and weights), students upload their source code as a ZIP archive, and a Firebase Cloud Function dispatches the code to **Google Gemini** to produce structured, criterion-by-criterion feedback. Lecturers can review, override, and finalize the results. The project was originally built as a four-person group assignment at TH Köln (Cologne University of Applied Sciences); this repository is a public fork prepared for portfolio purposes.

[![CI](https://github.com/mhelikopter/codescope/actions/workflows/ci.yml/badge.svg)](https://github.com/mhelikopter/codescope/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg)
![Compose Multiplatform](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4.svg)

## Screenshots

<!-- TODO: add screenshots — desktop dashboard, criteria catalog editor, analysis result view, Android dashboard -->

## Features

- **Role-based access**: Student, Lecturer, and Admin roles, enforced both in the UI and in Firestore security rules.
- **Criteria catalogs**: Lecturers build weighted question catalogs, optionally seeded by AI (`generateCriteria` cloud function).
- **ZIP-based project upload**: Students upload a project archive; the file type and size are validated client-side before upload.
- **AI code analysis**: A Firebase Cloud Function unpacks the ZIP, filters by allowed file extensions, builds a prompt with the catalog, and calls Gemini. The response is parsed into per-criterion scores and a weighted overall score.
- **Manual override**: Lecturers can revise scores and comments before publishing the assessment.
- **Cross-platform UI**: Single Compose Multiplatform codebase for Android and Desktop (Windows / macOS / Linux).
- **Native desktop distributables**: `.dmg`, `.msi`, and `.deb` produced via the Compose Desktop Gradle plugin.

## Tech stack

**Frontend (KMP client)**
- Kotlin 2.2 with Kotlin Multiplatform — `androidTarget` + `jvm`
- Compose Multiplatform 1.9
- Precompose (KMP-friendly navigation)
- Koin (dependency injection)
- Ktor client (CIO engine on JVM, Android engine on Android)
- kotlinx.serialization, kotlinx.coroutines, kotlinx.datetime
- BuildKonfig (compile-time injection of secrets from `local.properties`)

**Firebase integration**
- GitLive Firebase SDK on Android (Auth, Firestore, Storage, Functions)
- Direct **Firebase REST API** on Desktop/JVM, because the GitLive SDK does not target JVM reliably. Both platforms implement the same `FirestoreClient` / `AuthProvider` / `FirebaseStorageClient` / `RestClient` interfaces.

**Backend**
- Firebase Cloud Functions (TypeScript, Node)
- `@google/generative-ai` (Gemini 2.0 Flash)
- `adm-zip` for in-function ZIP unpacking
- Firestore + Cloud Storage as the data layer

**Tooling**
- Gradle Version Catalog
- Java 17 toolchain
- Dokka (multiplatform API docs)
- Compose Hot Reload during dev

## Architecture overview

CodeScope follows a clean-ish layered architecture inside `composeApp/commonMain`:

```
screens (Composables)
        |
        v
viewmodel (state holders)
        |
        v
logic (controllers — "Steuerung" classes)
        |
        v
data/repository (interfaces + impls)
        |
        v
data/client (Firestore / Storage / Auth / REST — expect/actual)
        |
        v
Firebase (Android SDK) | Firebase REST API (JVM)
```

For the full layered breakdown, the KMP Firebase split, the end-to-end analysis flow, and a summary of the security rules, see [`ARCHITECTURE.md`](./ARCHITECTURE.md).

## Project context & my contributions

CodeScope was built as a group project at **TH Köln**, course **SYP25**, **Team 06**, winter term **2025/2026**. Four core contributors landed roughly 342 commits between January and February 2026. This fork is maintained by **Maximilian Ehling** for portfolio purposes.

Within the team, my (Maximilian Ehling's) focus areas were:

- **The Kotlin Multiplatform split for Firebase.** GitLive Firebase works on Android but does not target JVM reliably, so I isolated all GitLive usage to `androidMain` and built a parallel Desktop data layer against the Firebase REST API. Both targets implement the same `commonMain` interfaces (`IFirestoreClient`, `IAuthProvider`, `IFirebaseStorageClient`, `IRestClient`).
- **The JVM client layer.** I authored `FirestoreClientImpl.kt`, `FirebaseStorageClientImpl.jvm.kt`, `AuthProviderImpl.kt`, and `RestClientImpl.jvm.kt` under `composeApp/src/jvmMain/kotlin/de/thkoeln/codescope/data/client/`.
- **Desktop login & auth.** Google OAuth flow on JVM (browser handoff + token exchange), login persistence, and robust Firestore JSON deserialization for the REST path.
- **Logic / controller layer.** Most-touched files: `KriterienkatalogSteuerung.kt`, `AnalyseSteuerung.kt`, `ProjektSteuerung.kt`, `AdminSteuerung.kt`, `LoginSteuerung.kt`. This included criteria catalog export/download, null-handling for analysis results, and a refactor of file management across platforms.
- **File handling & upload validation.** ZIP validation for student project uploads, criteria catalog file validation in `AnalysisConfigViewModel`, and a `readFileBytes` unification so commonMain no longer has to know about platform file APIs.
- **Desktop packaging.** macOS icon, bundle ID, vendor metadata, and native distribution targets (DMG / MSI / DEB) in `composeApp/build.gradle.kts`.
- **KDoc.** Documentation across the `logic/` controller layer so newcomers can navigate the orchestration code without re-reading every class.

Other areas (large parts of the UI screens, the Cloud Functions implementation, the Android-side Firebase wiring, and the assessment management flow) were owned by other teammates. I am not claiming the whole codebase.

## Getting started

### Prerequisites

- **JDK 17**
- **Android SDK** (compileSdk 36, minSdk 26, targetSdk 36) — install via Android Studio
- **A Firebase project of your own** with Auth, Firestore, Storage, and Cloud Functions enabled
- **Node.js + npm** if you want to build and deploy the Cloud Functions
- **Firebase CLI** (`npm install -g firebase-tools`) for deploying functions and rules

### Clone

```shell
git clone https://github.com/<your-fork>/codescope.git
cd codescope
```

## Configuration

CodeScope needs your own Firebase project. You will not be able to run it against the original TH Köln Firebase backend.

Create a `local.properties` file in the repository root (it is gitignored) and add:

```properties
# Google OAuth 2.0 client credentials for the Desktop login flow
CODE_SCOPE_DESKTOP_CLIENT_ID=your_oauth_client_id
CODE_SCOPE_DESKTOP_CLIENT_SECRET=your_oauth_client_secret

# Firebase project
CODE_SCOPE_FIREBASE_API_KEY=your_firebase_web_api_key
CODE_SCOPE_FIREBASE_PROJECT_ID=your-firebase-project-id
CODE_SCOPE_FIREBASE_STORAGE_BUCKET=your-firebase-project-id.appspot.com

# Region where you deployed the cloud functions
FIREBASE_FUNCTIONS_REGION=europe-west3
```

These values are read by the BuildKonfig plugin and injected into the binary at compile time. Never commit `local.properties`.

You will also need a `google-services.json` (Android, in `composeApp/`) configured against your Firebase project for the Android target.

For the Cloud Functions, set the Gemini key as a Firebase secret:

```shell
firebase functions:secrets:set GEMINI_API_KEY
```

## Build & run

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Then install the APK on a device or emulator. On Windows: `.\gradlew.bat :composeApp:assembleDebug`.

### Desktop (JVM)

```shell
./gradlew :composeApp:run
```

### Native desktop distributables

```shell
./gradlew :composeApp:createDistributable
```

This produces a `.dmg` on macOS, `.msi` on Windows, and `.deb` on Linux, configured in `compose.desktop { ... }` inside `composeApp/build.gradle.kts`.

### Cloud Functions

```shell
npm --prefix functions install
npm --prefix functions run build
firebase deploy --only functions
```

You can also deploy the security rules with `firebase deploy --only firestore:rules,storage:rules`.

## Android: SHA fingerprint

For the Android target to authenticate against your Firebase project, the SHA-1 fingerprint of your debug signing key must be registered under **Project settings → Your apps** in the Firebase console. To print the fingerprint of the default debug keystore:

**Windows (PowerShell):**
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**macOS / Linux:**
```shell
keytool -list -v -keystore "$HOME/.android/debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Copy the `SHA1` line into the Firebase console, then redownload `google-services.json` if it changes.

## Testing

Common-source unit tests run on both targets via the KMP `allTests` aggregate task:

```shell
./gradlew :composeApp:allTests
```

Test coverage is currently thin — it grew organically alongside the feature work in a 2-month group project. The CI workflow at `.github/workflows/ci.yml` runs `allTests` on every push.

## Project structure

```
team06/
├── composeApp/                         # KMP client
│   ├── build.gradle.kts                # Compose Desktop config, BuildKonfig, Dokka
│   └── src/
│       ├── commonMain/kotlin/de/thkoeln/codescope/
│       │   ├── screens/                # Composables (one file per screen)
│       │   ├── viewmodel/              # State holders, one per screen
│       │   ├── logic/                  # *Steuerung controllers + interfaces
│       │   ├── data/
│       │   │   ├── repository/         # *Verwaltung repository impls
│       │   │   └── client/             # IFirestoreClient, IAuthProvider, ...
│       │   ├── domain/                 # ai, analysis, course, criteria,
│       │   │                           # googleAuth, project, user
│       │   ├── di/                     # Koin modules
│       │   ├── components/             # Reusable UI fragments
│       │   ├── theme/
│       │   └── util/
│       ├── androidMain/                # Android Activity, GitLive Firebase impls
│       └── jvmMain/                    # Desktop entry, REST-based Firebase impls
├── functions/                          # Firebase Cloud Functions (TypeScript)
│   └── src/index.ts                    # analyseCode + generateCriteria
├── firestore.rules
├── storage.rules
└── ARCHITECTURE.md
```

## A note on naming

You will see German class names in the code: `Steuerung` = controller, `Verwaltung` = manager/repository, `Kriterienkatalog` = criteria catalog, `Dozent` = lecturer, `Kurs` = course. The project was originally developed in a German-language course and the team chose to keep the domain language German throughout the controller and repository layers. The names are kept verbatim for historical accuracy — they are not being retroactively renamed in this fork.

## License

Released under the **MIT License**. See `LICENSE` for the full text.

## Acknowledgements

CodeScope was originally developed as a group project at **TH Köln** in the course **SYP25**, **Team 06**, winter term **2025/2026**. Thanks to the rest of the team for the collaboration. This fork is maintained by **Maximilian Ehling** for portfolio and demonstration purposes and is not affiliated with TH Köln.
