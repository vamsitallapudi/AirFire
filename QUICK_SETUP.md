# AirFire - Quick Setup Guide (3-5 Days)

## Day 1: Fire TV Setup

1. **Create Android TV Project in Android Studio**
   - New Project → TV → Empty Activity
   - Min SDK: API 21
   - Package name: com.airfire.receiver

2. **Add permissions to AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

3. **Copy MainActivity.java** to your project

4. **Build and install on Fire TV:**
```bash
adb connect YOUR_FIRETV_IP
./gradlew installDebug
```

## Day 2: iOS Setup

1. **Create iOS Project in Xcode**
   - New Project → iOS App
   - Add ReplayKit capability

2. **Info.plist additions:**
```xml
<key>NSLocalNetworkUsageDescription</key>
<string>Stream to Fire TV</string>
```

3. **Create simple UI:**
   - Add one button "Start Mirroring"
   - Connect to IBAction

4. **Copy ViewController.swift** to your project

5. **IMPORTANT: Change the IP address in code to your Fire TV's IP**

## Day 3: Testing

1. **Find Fire TV IP:**
   - Settings → Network → Your WiFi → View Details

2. **Both devices on same WiFi network**

3. **Launch order:**
   - Start Fire TV app first
   - Then start iOS app
   - Tap "Start Mirroring"

## Troubleshooting Tips:

- **Black screen?** → Check H.264 NAL units in stream
- **Connection refused?** → Firewall/router settings
- **Lag?** → Lower bitrate in iOS code (change 5000000 to 2000000)

## Even Faster Alternatives:

1. **Use existing apps:**
   - AirScreen (Free on Fire TV)
   - Replica app

2. **Or modify this GitHub project:**
   - Search: "iOS screen mirror WebRTC"
   - Just change Android TV UI

## Limitations:
- In-app recording only (not system-wide)
- No audio
- Fixed resolution
- Manual IP entry

Total time: 3-5 days of actual coding!