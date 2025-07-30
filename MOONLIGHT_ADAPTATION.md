# Moonlight Adaptation for iPhone → Fire TV Mirroring

## Why Moonlight is Perfect:
- **Proven low-latency streaming** (used by millions)
- **Hardware-accelerated** encoding/decoding
- **Already handles network issues** (packet loss, etc.)
- **Cross-platform** (iOS, Android, etc.)
- **MIT License** - free to modify

## Repositories to Fork:
```bash
# iOS side (what captures and sends)
git clone https://github.com/moonlight-stream/moonlight-ios

# Android side (what receives on Fire TV)
git clone https://github.com/moonlight-stream/moonlight-android
```

## Key Modifications Needed:

### 1. iOS Side Changes (moonlight-ios):
**Current**: Receives game stream from PC
**Change to**: Capture iPhone screen using ReplayKit

**Files to modify:**
- `Limelight/Stream/VideoDecoderRenderer.m` → Change to video encoder
- Add ReplayKit capture instead of network receiver
- Remove game controller handling

**Estimated changes**: ~200 lines of code

### 2. Fire TV Side (moonlight-android):
**Current**: Displays game stream
**Keep**: The display and decoder parts (perfect as-is!)

**Files to modify:**
- `app/src/main/java/com/limelight/StreamInterface.java` → Remove game-specific features
- Keep all the video decoding (it's already optimized)

**Estimated changes**: ~50 lines of code

## Step-by-Step Adaptation:

### Day 1: Setup
```bash
# Clone both repos
git clone https://github.com/moonlight-stream/moonlight-ios
git clone https://github.com/moonlight-stream/moonlight-android

# Build both to ensure they work
# Follow their README instructions
```

### Day 2: iOS Modifications
```objc
// In moonlight-ios, replace network video receiver with:
// ReplayKit screen capture → H.264 encoder → Network sender

// Key file: Limelight/Stream/VideoDecoderRenderer.m
// Change from decoder to encoder
```

### Day 3: Fire TV Modifications
```java
// In moonlight-android, keep the decoder
// Remove game controller code
// Simplify UI for just "connect to iPhone"
```

## Advantages of Using Moonlight:

1. **Ultra-low latency** - Already optimized for real-time streaming
2. **Robust networking** - Handles WiFi issues, packet loss
3. **Hardware acceleration** - Uses GPU encoding/decoding
4. **Proven stability** - Used by millions of gamers
5. **Audio support** - Already built-in
6. **Multiple resolutions** - Adaptive quality

## The Smart Shortcut:

Instead of building from scratch, you're getting:
- 5+ years of optimization
- Professional-grade streaming stack
- Battle-tested networking code
- Hardware acceleration
- Cross-platform compatibility

**Time savings**: Instead of 3-6 months, you need 3-5 days to adapt existing code.

## Even Smarter Approach:

1. **Use Moonlight as-is** with a "fake game server"
2. Create a simple server on Mac/PC that captures iPhone screen
3. Stream to Moonlight Android on Fire TV
4. Zero mobile app development needed!

## Files You'll Actually Touch:
- iOS: ~5 files, ~200 lines changed
- Android: ~3 files, ~50 lines changed
- Total complexity: Much lower than starting from scratch

Want me to show you the specific code changes needed?