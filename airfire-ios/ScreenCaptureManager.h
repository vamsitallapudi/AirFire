//
//  ScreenCaptureManager.h
//  AirFire iOS Broadcaster
//
//  Created on 2025-01-30
//  Handles screen capture and H.264 encoding
//

#import <Foundation/Foundation.h>
#import <ReplayKit/ReplayKit.h>
#import <VideoToolbox/VideoToolbox.h>
#import <AVFoundation/AVFoundation.h>

@interface ScreenCaptureManager : NSObject

// Callback blocks
@property (nonatomic, copy) void (^onVideoFrame)(NSData *frameData);
@property (nonatomic, copy) void (^onError)(NSError *error);

// Screen capture control
- (void)startScreenCapture:(void(^)(BOOL success, NSError *error))completion;
- (void)stopScreenCapture;

// Configuration
@property (nonatomic) int targetBitrate;    // Default: 5000000 (5 Mbps)
@property (nonatomic) int targetFPS;        // Default: 30
@property (nonatomic) CGSize targetResolution; // Default: device resolution

@end