# AirFire Enhanced Build Instructions

## 🔥 AirPlay Receiver for Fire TV

The enhanced Fire TV app now supports both custom TCP streaming and **AirPlay receiver** functionality, allowing iOS devices to discover and connect via iOS Control Center.

## Build Steps

### 1. Prerequisites
- Android Studio installed
- Fire TV device on same WiFi network
- ADB debugging enabled on Fire TV

### 2. Build Enhanced App
```bash
cd /Users/vamsitallapudi/Documents/proj/AirFire/airfire-android

# Clean previous builds
./gradlew clean

# Build APK
./gradlew assembleDebug
```

### 3. Install on Fire TV
```bash
# Find your Fire TV IP (usually shown on screen)
adb connect 192.168.1.21

# Install the enhanced app
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.airfire/.SimpleAirFireActivity
```

## What's New in Enhanced Version

### 🎯 Dual Protocol Support
- **TCP Server**: Port 5000 (original custom protocol)
- **AirPlay Server**: Port 7000 (iOS compatible)

### 📱 iOS Integration
- Appears in iOS Control Center screen mirroring options
- Uses standard AirPlay protocol
- No custom iOS app needed

### 🖥️ Enhanced UI
- Shows both protocol endpoints
- Real-time connection status
- Protocol-specific indicators

## Testing Instructions

### Method 1: iOS Control Center (Standard)
1. **Launch app** on Fire TV
2. **Note the IP address** displayed (e.g., 192.168.1.21)
3. **On iPhone**: Swipe down from top-right to open Control Center
4. **Tap "Screen Mirroring"**
5. **Look for "AirFire"** in the device list
6. **Tap AirFire** to connect
7. **Verify** iPhone screen appears on Fire TV

### Method 2: Custom Protocol (Legacy)
1. Use the original iOS app with TCP connection to port 5000
2. App will show "📱 Custom TCP" connection

## Expected Behavior

### On Fire TV Screen:
```
🔥 AirFire for Fire TV
Fire TV IP: 192.168.1.21
📱 Custom TCP: 192.168.1.21:5000
🍎 AirPlay: 192.168.1.21:7000
📱 Custom app OR 🍎 iOS Control Center
```

### Status Updates:
- `🍎 AirPlay server ready on port 7000`
- `🍎 iOS device connected via AirPlay: [iPhone IP]`
- `🍎 AirPlay video data: [bytes] bytes`

## Troubleshooting

### AirFire not appearing in iOS Control Center
1. **Check WiFi**: Ensure iPhone and Fire TV on same network
2. **Restart app**: Force close and relaunch Fire TV app
3. **Check ports**: Verify no other app using port 7000
4. **iOS version**: Requires iOS 10+ for AirPlay discovery

### Connection successful but no video
1. **Check status messages** on Fire TV screen
2. **Look for**: `🍎 AirPlay video data: [bytes] bytes`
3. **Try different content**: Test with photos, videos, home screen

### Performance Issues
1. **WiFi strength**: Ensure strong WiFi signal
2. **Network traffic**: Avoid heavy downloads during streaming
3. **Fire TV resources**: Close other apps if needed

## Architecture Overview

```
iPhone (iOS Control Center)
         ↓ AirPlay Protocol
Fire TV App (Port 7000)
         ↓ Video Processing
     Display Surface
```

## Next Steps

1. **Build and install** enhanced Fire TV app
2. **Test AirPlay discovery** from iPhone Control Center
3. **Verify video streaming** works end-to-end
4. **Report results** for any additional optimizations needed

The iOS app is **no longer needed** - iOS devices can connect directly via Control Center using the standard AirPlay protocol.