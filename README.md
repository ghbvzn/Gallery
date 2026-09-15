# Gallery

An open-source Android gallery app built with Kotlin, Jetpack Compose, Room,
Coil, CameraX, and Media3. It organizes photos and videos by timeline, album,
location, and grid views, with search, favorites, private on-device tagging,
an in-app camera, and a restorable media Bin.

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android 7.0 (API 24) or newer for the app

## Build

1. Clone this repository and open it in Android Studio.
2. Run the `app` configuration, or build from a terminal:

   ```shell
   ./gradlew assembleDebug
   ```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Android's normal per-developer debug signing key is used, so no keystore needs
to be committed.

## Current version

Version 1.24.0 (version code 37) automatically tucks away gallery controls while scrolling and restores them after a short idle delay. The camera zoom slider now collapses after inactivity and reopens whenever pinch zoom is used, while all manual toggles remain available.

Version 1.23.0 (version code 36) adds collapsible gallery and camera controls. The Add Media button can shrink to an icon, the main timeline scrollbar can fold into an edge handle, and the camera zoom slider can collapse into a compact live zoom pill.

Version 1.22.0 (version code 35) restores camera controls after every video finalize
outcome, silently reconciles MediaStore changes made while the app was closed, prevents
destructive database fallback, and uses the maintained AndroidX EXIF parser. The test
SDK configuration is aligned with the app's standard Android 16 compile SDK.

## License

Licensed under the [MIT License](LICENSE).
