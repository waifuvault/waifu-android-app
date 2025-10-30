# WaifuVault Android App

Native Android application for uploading and sharing files using the WaifuVault API.

## Features

- **File Upload**: Upload files with customizable options
    - Password protection
    - One-time download
    - Hidden filenames
    - Custom expiry times
- **File Management**: View, share, and delete uploaded files
- **Native Android Integration**:
    - Share Sheet support (upload files from other apps)
    - Material Design 3 UI with Jetpack Compose
    - Adaptive chunk sizes based on network type
- **Secure**: Bucket token stored using DataStore

## Architecture

The app follows Clean Architecture principles with MVVM pattern:

```
presentation/    - UI layer (Jetpack Compose + ViewModels)
domain/          - Business logic (Models, Use Cases)
data/            - Data layer (API, Repository, Local Storage)
util/            - Utility classes
```

## Building

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Minimum SDK: API 24 (Android 7.0)

### Steps

1. Open the `/android` folder in Android Studio
2. Sync Gradle files
3. Build and run on emulator or device

### Gradle Build

```bash
cd android
./gradlew assembleDebug
```

## Configuration

### Bucket Token Setup

On first launch:

1. Tap the settings icon in the upload screen
2. Enter your WaifuVault bucket token
3. Token is securely stored using DataStore

You can create a bucket token via the WaifuVault API or use an existing one from your web app.

## API Integration

The app communicates directly with the WaifuVault REST API:

- **Base URL**: `https://waifuvault.moe`
- **No proxy required**: Direct API calls from mobile
- **Authentication**: Token-based (bucket token)

### Supported Operations

- Upload files with options
- Get file information
- Delete files
- Modify file properties
- Create and manage buckets
- Album support (future feature)

## Usage

### Upload a File

1. Tap "Choose File" on the upload screen
2. Select a file from your device
3. Configure upload options (optional):
    - Set password protection
    - Enable one-time download
    - Hide filename
    - Set expiry time
4. Tap the upload button
5. Copy or share the generated URL

### Share Files to WaifuVault

From any app with file sharing:

1. Tap "Share"
2. Select "WaifuVault"
3. File will be uploaded automatically
4. Share the generated URL

### Manage Files

1. Tap the list icon to view all uploaded files
2. Tap a file to expand details
3. Copy URL or share directly
4. Delete files you no longer need

## Network Optimization

The app automatically adjusts upload chunk sizes based on network type:

- **WiFi/Ethernet**: 10 MB chunks
- **Cellular**: 5 MB chunks
- **Slow 4G**: 2 MB chunks

## License

Same as the parent WaifuFiles project.
