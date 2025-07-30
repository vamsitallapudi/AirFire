# How to Test AirFire Android App

## 🔥 Quick Test Guide

### Prerequisites:
1. **Android Studio** installed
2. **Fire TV device** or Android TV emulator
3. **ADB** (Android Debug Bridge) set up

### Step 1: Build the App

```bash
# Navigate to project
cd /Users/vamsitallapudi/Documents/proj/AirFire/airfire-android

# Open in Android Studio
open .
```

**OR manually with command line:**

```bash
# Set Android SDK path (adjust path as needed)
export ANDROID_HOME=/Users/vamsitallapudi/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Build the app
./gradlew assembleDebug
```

### Step 2: Install on Fire TV

```bash
# Enable ADB on Fire TV:
# Settings → My Fire TV → Developer Options → ADB Debugging (ON)

# Connect to Fire TV via ADB
adb connect YOUR_FIRE_TV_IP  # e.g., adb connect 192.168.1.100

# Install the app
adb install app/build/outputs/apk/debug/app-debug.apk

# OR directly install from Android Studio using "Run" button
```

### Step 3: Test the App

1. **Launch AirFire** on Fire TV
   - Should show "🔥 AirFire for Fire TV" 
   - Displays Fire TV's IP address (e.g., 192.168.1.100:5000)
   - Status: "AirFire ready - Waiting for iPhone..."

2. **Test Network Connection**
   ```bash
   # From your computer, test if Fire TV is listening
   telnet FIRE_TV_IP 5000
   # Should connect successfully
   ```

3. **Test with Simple Client**
   ```bash
   # Send test data to Fire TV
   echo "Hello from iPhone!" | nc FIRE_TV_IP 5000
   ```
   
   Fire TV should show: "📡 Receiving data: X bytes"

## What You Should See:

### 🟢 **Success Indicators:**
- App launches without crashing
- Shows Fire TV IP address clearly
- Status updates show "AirFire ready on port 5000"
- Network connections are accepted
- Surface view is visible (black area for future video)

### 🔴 **Potential Issues:**
- **"SDK location not found"** → Install Android Studio & set ANDROID_HOME
- **App crashes on launch** → Check logcat: `adb logcat | grep AirFire`
- **"Permission denied"** → Enable Developer Options on Fire TV
- **Can't connect to port 5000** → Check Fire TV firewall settings

## Testing Without Fire TV:

### Use Android Emulator:
1. Open Android Studio
2. Create Android TV emulator (API 30+)
3. Install and test app on emulator
4. Works exactly the same as real Fire TV

### Test Network with Computer:
```bash
# Simple test server (if no Fire TV available)
python3 -c "
import socket
s = socket.socket()
s.bind(('0.0.0.0', 5000))
s.listen(1)
print('Test server listening on port 5000...')
conn, addr = s.accept()
print(f'Connected by {addr}')
while True:
    data = conn.recv(1024)
    if not data: break
    print(f'Received: {len(data)} bytes')
conn.close()
"
```

## Next Steps After Testing:

Once the Android app is working:

1. ✅ **Android receiver ready** - Shows IP, accepts connections
2. 🔄 **Build iOS broadcaster** - Capture screen and connect to Fire TV
3. 🎬 **Add video decoding** - Integrate Moonlight's MediaCodec decoder
4. 🚀 **Full end-to-end testing** - iPhone → Fire TV screen mirroring

## Quick Verification Checklist:

- [ ] App installs without errors
- [ ] Shows Fire TV IP address  
- [ ] Status shows "ready" state
- [ ] Can connect via telnet/nc to port 5000
- [ ] App doesn't crash when connection made
- [ ] Surface view appears (for future video)

**Time to test: ~15 minutes (if Android Studio already installed)**

Ready to build the iOS side once Android is working! 🚀