# Fire TV Deployment Guide

## Overview
This guide covers deploying the AirFire Android app to Amazon Fire TV devices for AirPlay receiving functionality.

## Prerequisites
- Android SDK with ADB tools installed
- Fire TV device on same network
- Development machine (Windows/Mac/Linux)

## Fire TV App Configuration
The app is already optimized for Fire TV with:
- ✅ Leanback UI support (`android.software.leanback`)
- ✅ TV banner for launcher (`@drawable/atv_banner`)
- ✅ Landscape orientation enforced
- ✅ Remote control navigation support
- ✅ No touchscreen requirement (`android.hardware.touchscreen` required="false")

## Deployment Steps

### 1. Build APK

**Windows:**
```bash
cd "E:\Proj\AirFire\airfire-android"
./gradlew assembleDebug
```

**Mac:**
```bash
cd /path/to/AirFire/airfire-android
./gradlew assembleDebug
```

**Output Location:** `app/build/outputs/apk/debug/app-debug.apk`

### 2. Enable Fire TV Developer Options

On Fire TV device:
1. **Settings** → **My Fire TV** → **About**
2. Click **Build** 7 times rapidly until "Developer options enabled"
3. **Settings** → **My Fire TV** → **Developer Options**
4. Enable:
   - **ADB Debugging**
   - **Apps from unknown sources**
5. Note the **IP address** shown in Developer Options

### 3. ADB Connection and Installation

**Connect to Fire TV:**
```bash
# Replace IP_ADDRESS with your Fire TV's IP
adb connect IP_ADDRESS:5555
```

**Install APK:**
```bash
# First installation
adb install "path/to/app-debug.apk"

# Reinstall (if already exists)
adb install -r "path/to/app-debug.apk"

# Or uninstall first
adb uninstall com.airfire.debug
adb install "path/to/app-debug.apk"
```

**Launch App:**
```bash
adb shell am start -n com.airfire.debug/com.airfire.SimpleAirFireActivity
```

### 4. Platform-Specific Commands

**Windows:**
```bash
# Install
adb install "E:\Proj\AirFire\airfire-android\app\build\outputs\apk\debug\app-debug.apk"

# Launch
adb shell am start -n com.airfire.debug/com.airfire.SimpleAirFireActivity
```

**Mac:**
```bash
# Install (adjust path as needed)
adb install "/Users/username/Projects/AirFire/airfire-android/app/build/outputs/apk/debug/app-debug.apk"

# Launch
adb shell am start -n com.airfire.debug/com.airfire.SimpleAirFireActivity
```

### 5. Testing AirPlay Connection

1. **Ensure same WiFi network** for iOS device and Fire TV
2. **iOS Control Center** → **Screen Mirroring**
3. **Select "AirFire"** from device list
4. **Start mirroring** to Fire TV

### 6. Troubleshooting

**Check app status:**
```bash
adb shell ps | grep airfire
```

**View logs:**
```bash
adb logcat | grep AirFire
```

**Reconnect ADB:**
```bash
adb disconnect
adb connect IP_ADDRESS:5555
```

**Alternative Installation Methods:**
- **Downloader app** on Fire TV with cloud storage link
- **ES File Explorer** with USB/network storage
- **Fire TV remote app** file transfer

## App Features on Fire TV

- **Full landscape UI** optimized for TV screens
- **Remote control navigation** support
- **AirPlay receiver** functionality
- **iOS device discovery** on local network
- **Video/audio streaming** from iOS devices
- **Fire TV launcher integration** with custom banner

## Build Configuration Notes

The app uses:
- **Target/Compile SDK:** 34
- **Min SDK:** 21 (supports older Fire TV models)
- **Package ID:** `com.airfire.debug` (debug) / `com.airfire` (release)
- **Main Activity:** `SimpleAirFireActivity`
- **Orientation:** Landscape only
- **Theme:** Fullscreen, no title bar

## Development Workflow

1. **Make code changes** on any platform
2. **Build APK** using Gradle
3. **Install via ADB** to Fire TV
4. **Test AirPlay functionality** with iOS device
5. **Check logs** for debugging if needed

## Release Notes

- Fixed string escaping issues in `AirPlayHTTPHandler.java`
- Optimized for Fire TV with proper manifest configuration
- Added TV launcher banner and leanback support
- Verified compatibility with Fire TV remote navigation