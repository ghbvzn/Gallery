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

Version 1.16.0 (version code 29) moves the photo aspect-ratio and video quality/FPS
controls to the top camera bar. Setting changes now rebind the active CameraX use
cases, photo ratios are applied to the saved JPEG crop, and selected video quality
and frame-rate ranges are applied to the actual recording pipeline.

## License

Licensed under the [MIT License](LICENSE).
