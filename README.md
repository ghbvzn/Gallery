# Gallery

An open-source Android gallery app built with Kotlin, Jetpack Compose, Room,
Coil, CameraX, and Media3. It organizes photos and videos by timeline, album,
location, and grid views, with search, favorites, tagging, an in-app camera,
and optional Gemini-powered tag suggestions.

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android 7.0 (API 24) or newer for the app

## Build

1. Clone this repository and open it in Android Studio.
2. Copy `.env.example` to `.env`.
3. Replace `MY_GEMINI_API_KEY` if you want AI tag suggestions. Keep `.env`
   private; it is ignored by Git.
4. Run the `app` configuration, or build from a terminal:

   ```shell
   ./gradlew assembleDebug
   ```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Android's normal per-developer debug signing key is used, so no keystore needs
to be committed.

## Current version

Version 1.8.2 (version code 19) includes performance, media-sync, thumbnail
memory, video playback, search, and bottom-navigation improvements.

## License

Licensed under the [MIT License](LICENSE).
