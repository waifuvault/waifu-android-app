# WaifuVault Android App

Native Android application for uploading files using the WaifuVault API.

## Features

- **File Upload**: Upload files with customizable options
    - Password protection
    - One-time download
    - Hidden filenames
    - Custom expiry times
- **Native Android Integration**:
    - Material Design 3 UI with Jetpack Compose
    - Modern Android architecture

## Architecture

The app follows Clean Architecture principles with MVVM pattern:

```
presentation/    - UI layer (Jetpack Compose + ViewModels)
domain/          - Business logic (Models, Use Cases)
data/            - Data layer (API, Repository)
util/            - Utility classes
```

## Building

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Minimum SDK: API 24 (Android 7.0)

### Steps

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on emulator or device

### Gradle Build

```bash
./gradlew assembleDebug
```

## API Integration

The app communicates directly with the WaifuVault REST API:

- **Base URL**: `https://waifuvault.moe`
- **Direct API calls**: No proxy required

### Supported Operations

- Upload files with configurable options (password, expiry, hide filename, one-time download)

## Usage

### Upload a File

1. Launch the app
2. Tap "Choose File" on the upload screen
3. Select a file from your device
4. Configure upload options (optional):
    - Set password protection
    - Enable one-time download
    - Hide filename
    - Set expiry time (e.g., "1h", "30m", "2d")
5. Tap the upload button
6. Copy the generated URL to share your file

## License

Same as the parent WaifuVault project.
