# 🔥 AirFire - iPhone to Fire TV Screen Mirroring

Stream your iPhone screen directly to Amazon Fire TV using the standard AirPlay protocol.

## 🎯 What This Is

AirFire transforms your Fire TV into an AirPlay receiver, allowing iPhones to discover and connect via iOS Control Center - just like commercial apps such as AirScreen and DoCast.

## 📁 Project Structure

```
AirFire/
├── airfire-android/           # Enhanced Fire TV app with AirPlay receiver
│   ├── app/src/main/java/com/airfire/
│   │   ├── SimpleAirFireActivity.java    # Main activity with dual protocol support
│   │   ├── AirPlayReceiver.java          # AirPlay server implementation
│   │   └── AirPlayHTTPHandler.java       # AirPlay protocol handler
│   └── app/src/main/res/layout/
│       └── activity_simple_airfire.xml   # Enhanced UI layout
├── BUILD_AIRPLAY.md           # Build and deployment instructions
└── README.md                  # This file
```

## ⚡ Quick Start

### 1. Build and Install Fire TV App
```bash
cd airfire-android
./gradlew assembleDebug
adb connect [FIRE_TV_IP]
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Connect from iPhone
1. Launch AirFire app on Fire TV
2. On iPhone: Swipe down from top-right → Control Center
3. Tap "Screen Mirroring"
4. Select "AirFire" from device list
5. Start mirroring!

## 🔧 Technical Architecture

### Dual Protocol Support
- **AirPlay Server** (Port 7000): Standard iOS screen mirroring
- **TCP Server** (Port 5000): Legacy custom protocol support

### AirPlay Implementation
- **mDNS/Bonjour**: Service discovery for iOS Control Center
- **HTTP Server**: Handles AirPlay protocol requests
- **Video Processing**: H.264 stream decoding and display

## 📱 Supported Devices

- **iOS**: iPhone/iPad with iOS 10+ (AirPlay compatible)
- **Fire TV**: All Amazon Fire TV devices with WiFi connectivity

## 🚀 Key Features

- ✅ **No custom iOS app needed** - uses standard iOS Control Center
- ✅ **Appears in iOS device list** like commercial mirroring apps
- ✅ **Dual protocol support** for legacy and standard connections
- ✅ **Real-time status updates** on Fire TV screen
- ✅ **WiFi-based streaming** with automatic discovery

## 🔍 How It Works

1. **Fire TV App** starts AirPlay server on port 7000
2. **Bonjour/mDNS** advertises service to local network
3. **iPhone discovers** "AirFire" in Control Center device list
4. **AirPlay connection** established using standard protocol
5. **Video stream** decoded and displayed on Fire TV

## 📋 Build Requirements

- Android Studio
- Fire TV with ADB debugging enabled
- iPhone and Fire TV on same WiFi network

## 🎯 Why AirPlay vs Custom App?

**Previous Approach (ReplayKit):**
- Required custom iOS app
- iOS 18 limitations prevented video buffer access
- Complex setup and permissions

**Current Approach (AirPlay):**
- Uses standard iOS screen mirroring
- Appears in Control Center like commercial apps
- No iOS app installation needed
- Works with all iOS versions 10+

## 🏗️ Development History

This project evolved from a custom ReplayKit-based solution to a standard AirPlay receiver implementation after discovering iOS 18 limitations with video buffer access in ReplayKit framework.

## 📚 References

- [AirPlay Protocol Specification](https://nto.github.io/AirPlay.html)
- [mDNS Service Discovery](https://developer.apple.com/bonjour/)
- [Fire TV Development Guide](https://developer.amazon.com/docs/fire-tv/getting-started-developing-apps-and-games.html)