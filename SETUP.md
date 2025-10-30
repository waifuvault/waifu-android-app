# Android App Build Setup

## Prerequisites

The Android app requires:

- Java 25 (you have this)
- Android SDK (needs installation)

## Setup Options

### Option 1: Android Studio (Recommended)

1. Install Android Studio from JetBrains Toolbox
2. Open Android Studio
3. File → Open → Select `/android` folder
4. Android Studio will auto-download SDK and build tools
5. Build → Make Project

### Option 2: Manual SDK Installation

1. Download Android Command Line Tools:

```bash
cd ~
mkdir -p android-sdk/cmdline-tools
cd android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
```

2. Set environment variables:

```bash
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

3. Accept licenses and install SDK:

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

4. Create local.properties:

```bash
echo "sdk.dir=$HOME/android-sdk" > /home/vic/IdeaProjects/WaifuFiles/android/local.properties
```

5. Build the app:

```bash
cd /home/vic/IdeaProjects/WaifuFiles/android
./gradlew assembleDebug
```

## Quick Start (Android Studio)

Since you already have JetBrains Toolbox, the fastest path is:

1. JetBrains Toolbox → Install "Android Studio"
2. Open `/home/vic/IdeaProjects/WaifuFiles/android`
3. Let Android Studio handle everything
4. Click "Build" or "Run"

This is the standard workflow for Android development!