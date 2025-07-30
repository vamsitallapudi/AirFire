# Moonlight iPhone → Fire TV Adaptation - Detailed Implementation Plan

## Project Overview
Transform Moonlight from **PC game streaming** to **iPhone screen mirroring** to Fire TV.

## Current Architecture vs Target Architecture

### Current Moonlight Flow:
```
PC/Mac Game → Nvidia GameStream → Network → Moonlight Client → Display
```

### Target AirFire Flow:
```
iPhone Screen → ReplayKit Capture → H.264 Encode → Network → Fire TV → Display
```

## Key Modifications Required

### 1. iOS Side - Transform Receiver to Broadcaster

**Current**: iOS receives H.264 stream and decodes
**Target**: iOS captures screen and encodes H.264 stream

**Files to Modify**:
- `Limelight/Stream/VideoDecoderRenderer.h/.m` → Convert to VideoEncoderBroadcaster
- `Limelight/Stream/StreamManager.h/.m` → Update for broadcasting mode
- `Limelight/Input/StreamView.h/.m` → Simplify (no game input needed)

**Key Changes**:
1. **Replace VideoDecoderRenderer**:
   ```objc
   // OLD: Receives and decodes video
   - (int)submitDecodeBuffer:(unsigned char *)data length:(int)length;
   
   // NEW: Captures and encodes video
   - (void)captureScreenWithReplayKit;
   - (int)encodeAndTransmitFrame:(CMSampleBuffer *)sampleBuffer;
   ```

2. **Add ReplayKit Integration**:
   ```objc
   #import <ReplayKit/ReplayKit.h>
   #import <VideoToolbox/VideoToolbox.h>
   
   @interface VideoEncoderBroadcaster : NSObject
   @property (strong, nonatomic) RPScreenRecorder *screenRecorder;
   @property (nonatomic) VTCompressionSession *compressionSession;
   @end
   ```

3. **Networking - Reverse Direction**:
   ```objc
   // OLD: Client connects to server
   // NEW: Broadcaster connects to Fire TV receiver
   ```

### 2. Android Side - Keep Decoder, Simplify UI

**Current**: Android receives stream and decodes (PERFECT!)
**Target**: Same, but remove game-specific features

**Files to Modify**:
- `MainActivity.java` → Simplify for TV-only interface
- Remove game controller handling
- Keep all the video decoding (it's already optimized!)

**Key Changes**:
1. **Simplify UI** - Remove computer discovery, just show "Waiting for iPhone"
2. **Remove Input Handling** - No game controllers needed
3. **Keep Decoder Intact** - The MediaCodecDecoderRenderer is perfect as-is!

## Implementation Strategy

### Phase 1: Android Fire TV App (Day 1)
1. **Clone moonlight-android**
2. **Simplify MainActivity**:
   ```java
   // Remove computer selection UI
   // Add simple "Waiting for connection" screen
   // Keep all MediaCodec decoding logic
   ```
3. **Remove Input Systems**:
   ```java
   // Remove controller handling
   // Remove input capture
   // Keep only video decoding pipeline
   ```

### Phase 2: iOS Broadcaster App (Day 2-3)
1. **Clone moonlight-ios**
2. **Create new iOS project** using Moonlight's networking
3. **Replace VideoDecoderRenderer** with screen capture:
   ```objc
   // ReplayKit screen capture
   // VideoToolbox H.264 encoding  
   // Use Moonlight's network stack (reversed)
   ```

### Phase 3: Integration (Day 4-5)
1. **Test basic connection**
2. **Optimize encoding/decoding parameters**
3. **Handle disconnections gracefully**

## Advantages of Using Moonlight

### 1. **Proven Network Stack**
- Sub-50ms latency optimization
- Packet loss handling
- Adaptive bitrate
- Connection recovery

### 2. **Hardware Acceleration**
- iOS: VideoToolbox encoding
- Android: MediaCodec decoding
- Both use GPU acceleration

### 3. **Mature Codebase**
- 5+ years of optimization
- Used by millions
- Professional error handling
- Cross-platform compatibility

## Specific Code Changes

### iOS VideoEncoderBroadcaster.h
```objc
@interface VideoEncoderBroadcaster : NSObject
- (id)initWithFireTVIP:(NSString*)ip port:(int)port;
- (void)startScreenCapture;
- (void)stopScreenCapture;
@end
```

### iOS VideoEncoderBroadcaster.m  
```objc
- (void)startScreenCapture {
    [[RPScreenRecorder sharedRecorder] startCaptureWithHandler:^(CMSampleBuffer *sampleBuffer, RPSampleBufferType bufferType, NSError *error) {
        if (bufferType == RPSampleBufferTypeVideo) {
            [self encodeAndTransmitFrame:sampleBuffer];
        }
    }];
}

- (void)encodeAndTransmitFrame:(CMSampleBuffer *)sampleBuffer {
    // Use VideoToolbox to encode H.264
    // Send over Moonlight's network stack
}
```

### Android MainActivity.java Changes
```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_waiting_screen);
        
        // Start TCP server for iPhone connection
        startVideoReceiver();
    }
    
    private void startVideoReceiver() {
        // Use existing MediaCodecDecoderRenderer
        // Just wait for iPhone connection instead of PC
    }
}
```

## Time Estimate: 4-5 Days Total

### Day 1: Android simplification (4-6 hours)
### Day 2: iOS screen capture integration (6-8 hours)  
### Day 3: iOS networking adaptation (4-6 hours)
### Day 4: Integration and testing (4-6 hours)
### Day 5: Polish and optimization (2-4 hours)

## Why This Approach is Optimal

1. **90% of the hard work is done** - Moonlight handles all the complex streaming
2. **Battle-tested performance** - Sub-50ms latency already achieved
3. **Hardware acceleration** - Both encoding and decoding optimized
4. **Error handling** - Professional-grade network recovery
5. **Cross-platform** - iOS and Android compatibility proven

The result will be a **professional-quality screen mirroring solution** that performs better than most commercial apps, built in under a week!