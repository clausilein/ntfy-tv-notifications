# Open Source Attributions

This application uses the following open source libraries. We are grateful to the authors and contributors of these projects.

---

## Core Libraries

### Kotlin
- **License**: Apache License 2.0
- **Copyright**: JetBrains s.r.o. and Kotlin Programming Language contributors
- **Repository**: https://github.com/JetBrains/kotlin
- **Used for**: Programming language for the entire application

### Kotlin Coroutines
- **License**: Apache License 2.0
- **Copyright**: JetBrains s.r.o.
- **Repository**: https://github.com/Kotlin/kotlinx.coroutines
- **Components**:
  - `kotlinx-coroutines-android`
  - `kotlinx-coroutines-core`
- **Used for**: Asynchronous programming and reactive streams

---

## Android Jetpack Components

All AndroidX libraries are licensed under the Apache License 2.0 and are copyright The Android Open Source Project.

### AndroidX Core
- **Component**: `androidx.core:core-ktx`
- **Repository**: https://android.googlesource.com/platform/frameworks/support
- **Used for**: Kotlin extensions for Android framework APIs

### AndroidX AppCompat
- **Component**: `androidx.appcompat:appcompat`
- **Used for**: Backward-compatible support for Android APIs

### AndroidX Activity
- **Component**: `androidx.activity:activity-compose`
- **Used for**: Activity lifecycle and Compose integration

### AndroidX Lifecycle
- **Component**: `androidx.lifecycle:lifecycle-runtime-ktx`
- **Used for**: Lifecycle-aware components and reactive data handling

### AndroidX SavedState
- **Components**: Included with lifecycle libraries
- **Used for**: State preservation across configuration changes

---

## Jetpack Compose

All Compose libraries are licensed under the Apache License 2.0 and are copyright The Android Open Source Project.

### Compose BOM (Bill of Materials)
- **Component**: `androidx.compose:compose-bom`
- **Repository**: https://android.googlesource.com/platform/frameworks/support
- **Used for**: Version alignment of Compose libraries

### Compose UI
- **Components**:
  - `androidx.compose.ui:ui`
  - `androidx.compose.ui:ui-graphics`
  - `androidx.compose.ui:ui-tooling-preview`
- **Used for**: Declarative UI framework and graphics

### Compose TV Material 3
- **Components**:
  - `androidx.tv:tv-foundation`
  - `androidx.tv:tv-material`
- **Repository**: https://android.googlesource.com/platform/frameworks/support
- **Used for**: Android TV optimized Material Design 3 components

### Compose Tooling (Debug)
- **Components**:
  - `androidx.compose.ui:ui-tooling` (debug)
  - `androidx.compose.ui:ui-test-manifest` (debug)
- **Used for**: Development tools and preview functionality

---

## Networking

### OkHttp
- **License**: Apache License 2.0
- **Copyright**: Square, Inc.
- **Repository**: https://github.com/square/okhttp
- **Component**: `com.squareup.okhttp3:okhttp`
- **Used for**: WebSocket client for real-time ntfy.sh connections
- **Version**: 4.x

**Note**: OkHttp is a critical component enabling the real-time notification functionality of this app.

---

## Database & Persistence

### Room Database
- **License**: Apache License 2.0
- **Copyright**: The Android Open Source Project
- **Repository**: https://android.googlesource.com/platform/frameworks/support
- **Components**:
  - `androidx.room:room-runtime`
  - `androidx.room:room-ktx`
  - `androidx.room:room-compiler` (annotation processor)
- **Used for**: Local database for subscription and message persistence

---

## Background Processing

### WorkManager
- **License**: Apache License 2.0
- **Copyright**: The Android Open Source Project
- **Repository**: https://android.googlesource.com/platform/frameworks/support
- **Component**: `androidx.work:work-runtime-ktx`
- **Used for**: Background task scheduling (present in dependencies, not primary mechanism)

---

## Testing Libraries (Not included in release build)

### Compose UI Test
- **Component**: `androidx.compose.ui:ui-test-junit4`
- **License**: Apache License 2.0
- **Used for**: UI testing with Compose

---

## Build Tools & Plugins

### Kotlin Symbol Processing (KSP)
- **License**: Apache License 2.0
- **Copyright**: Google LLC
- **Repository**: https://github.com/google/ksp
- **Used for**: Annotation processing for Room database

### Android Gradle Plugin
- **License**: Apache License 2.0
- **Copyright**: The Android Open Source Project
- **Used for**: Building and packaging the Android application

### Kotlin Gradle Plugin
- **License**: Apache License 2.0
- **Copyright**: JetBrains s.r.o.
- **Used for**: Kotlin compilation and build configuration

---

## Third-Party Services

### ntfy.sh
- **Service**: Real-time notification delivery
- **Website**: https://ntfy.sh
- **GitHub**: https://github.com/binwiederhier/ntfy
- **License**: Apache License 2.0 / GPL 2.0
- **Used for**: Backend notification service (user-configured)

**Note**: This app connects to ntfy.sh servers based on user configuration. ntfy.sh is an independent third-party service not affiliated with this application. Users are responsible for their use of ntfy.sh and should review their [privacy policy](https://ntfy.sh/docs/privacy/) and [terms of service](https://ntfy.sh/docs/tos/).

---

## License Texts

### Apache License 2.0

The Apache License 2.0 is used by most dependencies in this project. The full text is available at:
- https://www.apache.org/licenses/LICENSE-2.0
- In this repository: [LICENSE](LICENSE) (for this app's code)

**Summary**: The Apache License 2.0 is a permissive free software license that allows you to use, modify, and distribute the software with minimal restrictions. It requires preservation of copyright and license notices, but does not require derivative works to use the same license.

---

## How to View Licenses in the App

When this app is published, you can view all open source licenses by:
1. Opening the app
2. Navigate to Settings
3. Select "Open Source Licenses"

This will display an automatically generated list of all libraries and their licenses.

---

## Contributing

If you notice any missing attributions or licensing issues, please:
1. Open an issue: https://github.com/clausilein/ntfy-tv-notifications/issues
2. Email: android-ntfy@clausr.net

We take open source licensing seriously and will promptly address any concerns.

---

## Acknowledgments

Special thanks to:
- **JetBrains** for Kotlin and IntelliJ IDEA
- **Google** for Android, Jetpack, and Compose
- **Square** for OkHttp
- **Philipp C. Heckel** for ntfy.sh
- **The open source community** for making projects like this possible

This application would not be possible without the incredible work of these developers and organizations.

---

**Last Updated**: January 2025

For the most up-to-date list of dependencies, see:
- [`build.gradle.kts`](app/build.gradle.kts)
- [`libs.versions.toml`](gradle/libs.versions.toml)
