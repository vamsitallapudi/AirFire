//
//  ScreenCaptureManager.m
//  AirFire iOS Broadcaster
//
//  Created on 2025-01-30
//  Handles screen capture and H.264 encoding
//

#import "ScreenCaptureManager.h"

@interface ScreenCaptureManager ()
@property (nonatomic) VTCompressionSessionRef compressionSession;
@property (nonatomic, strong) dispatch_queue_t encodingQueue;
@property (nonatomic) BOOL isCapturing;
@end

@implementation ScreenCaptureManager

- (instancetype)init {
    self = [super init];
    if (self) {
        // Default settings optimized for Fire TV streaming
        self.targetBitrate = 5000000;  // 5 Mbps
        self.targetFPS = 30;
        self.targetResolution = [UIScreen mainScreen].bounds.size;
        
        // Create encoding queue
        self.encodingQueue = dispatch_queue_create("com.airfire.encoding", DISPATCH_QUEUE_SERIAL);
        self.isCapturing = NO;
    }
    return self;
}

- (void)dealloc {
    [self stopScreenCapture];
    [self cleanupCompressionSession];
}

#pragma mark - Public Methods

- (void)startScreenCapture:(void(^)(BOOL success, NSError *error))completion {
    if (self.isCapturing) {
        completion(NO, [NSError errorWithDomain:@"AirFireError" code:1001 userInfo:@{NSLocalizedDescriptionKey: @"Already capturing"}]);
        return;
    }
    
    // Setup H.264 compression session
    OSStatus status = [self setupCompressionSession];
    if (status != noErr) {
        NSError *error = [NSError errorWithDomain:@"AirFireError" code:1002 userInfo:@{NSLocalizedDescriptionKey: @"Failed to setup H.264 encoder"}];
        completion(NO, error);
        return;
    }
    
    // Start ReplayKit screen capture
    RPScreenRecorder *recorder = [RPScreenRecorder sharedRecorder];
    
    if (!recorder.isAvailable) {
        NSError *error = [NSError errorWithDomain:@"AirFireError" code:1003 userInfo:@{NSLocalizedDescriptionKey: @"Screen recording not available"}];
        completion(NO, error);
        return;
    }
    
    __weak typeof(self) weakSelf = self;
    [recorder startCaptureWithHandler:^(CMSampleBufferRef sampleBuffer, RPSampleBufferType bufferType, NSError *error) {
        if (error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (weakSelf.onError) {
                    weakSelf.onError(error);
                }
            });
            return;
        }
        
        if (bufferType == RPSampleBufferTypeVideo) {
            [weakSelf processSampleBuffer:sampleBuffer];
        }
    } completionHandler:^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (error) {
                completion(NO, error);
            } else {
                weakSelf.isCapturing = YES;
                completion(YES, nil);
            }
        });
    }];
}

- (void)stopScreenCapture {
    if (!self.isCapturing) {
        return;
    }
    
    [[RPScreenRecorder sharedRecorder] stopCaptureWithHandler:^(NSError *error) {
        if (error) {
            NSLog(@"Error stopping screen capture: %@", error.localizedDescription);
        }
    }];
    
    [self cleanupCompressionSession];
    self.isCapturing = NO;
}

#pragma mark - Private Methods

- (OSStatus)setupCompressionSession {
    [self cleanupCompressionSession];
    
    // Get screen dimensions
    CGSize screenSize = [UIScreen mainScreen].bounds.size;
    CGFloat scale = [UIScreen mainScreen].scale;
    
    int width = (int)(screenSize.width * scale);
    int height = (int)(screenSize.height * scale);
    
    // Ensure dimensions are even (required for H.264)
    width = (width + 1) & ~1;
    height = (height + 1) & ~1;
    
    // Create compression session
    OSStatus status = VTCompressionSessionCreate(
        kCFAllocatorDefault,
        width, height,
        kCMVideoCodecType_H264,
        NULL, // encoder specification
        NULL, // source image buffer attributes
        kCFAllocatorDefault, // compressed data allocator
        compressionOutputCallback, // output callback
        (__bridge void *)self, // callback reference
        &_compressionSession
    );
    
    if (status != noErr) {
        NSLog(@"Failed to create compression session: %d", (int)status);
        return status;
    }
    
    // Configure encoder properties
    [self configureCompressionSession];
    
    // Prepare to encode
    status = VTCompressionSessionPrepareToEncodeFrames(_compressionSession);
    if (status != noErr) {
        NSLog(@"Failed to prepare compression session: %d", (int)status);
        [self cleanupCompressionSession];
        return status;
    }
    
    return noErr;
}

- (void)configureCompressionSession {
    // Set real-time encoding
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_RealTime, kCFBooleanTrue);
    
    // Set profile level
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_ProfileLevel, kVTProfileLevel_H264_Main_AutoLevel);
    
    // Set bitrate
    CFNumberRef bitrate = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, &_targetBitrate);
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_AverageBitRate, bitrate);
    CFRelease(bitrate);
    
    // Set max keyframe interval (1 second)
    int keyFrameInterval = self.targetFPS;
    CFNumberRef keyFrameIntervalRef = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, &keyFrameInterval);
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_MaxKeyFrameInterval, keyFrameIntervalRef);
    CFRelease(keyFrameIntervalRef);
    
    // Set expected frame rate
    CFNumberRef frameRate = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, &_targetFPS);
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_ExpectedFrameRate, frameRate);
    CFRelease(frameRate);
    
    // Enable hardware acceleration if available
    VTSessionSetProperty(_compressionSession, kVTCompressionPropertyKey_UsingHardwareAcceleratedVideoEncoder, kCFBooleanTrue);
}

- (void)processSampleBuffer:(CMSampleBufferRef)sampleBuffer {
    if (!self.isCapturing || !_compressionSession) {
        return;
    }
    
    dispatch_async(self.encodingQueue, ^{
        CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        if (!imageBuffer) {
            return;
        }
        
        // Get presentation timestamp
        CMTime presentationTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer);
        
        // Encode frame
        OSStatus status = VTCompressionSessionEncodeFrame(
            self->_compressionSession,
            imageBuffer,
            presentationTime,
            kCMTimeInvalid, // duration
            NULL, // frame properties
            NULL, // source frame reference
            NULL  // info flags out
        );
        
        if (status != noErr) {
            NSLog(@"Failed to encode frame: %d", (int)status);
        }
    });
}

- (void)cleanupCompressionSession {
    if (_compressionSession) {
        VTCompressionSessionCompleteFrames(_compressionSession, kCMTimeInvalid);
        VTCompressionSessionInvalidate(_compressionSession);
        CFRelease(_compressionSession);
        _compressionSession = NULL;
    }
}

#pragma mark - VideoToolbox Callback

void compressionOutputCallback(
    void *outputCallbackRefCon,
    void *sourceFrameRefCon,
    OSStatus status,
    VTEncodeInfoFlags infoFlags,
    CMSampleBufferRef sampleBuffer
) {
    if (status != noErr) {
        NSLog(@"Compression callback error: %d", (int)status);
        return;
    }
    
    if (!sampleBuffer) {
        return;
    }
    
    ScreenCaptureManager *manager = (__bridge ScreenCaptureManager *)outputCallbackRefCon;
    
    // Extract H.264 data
    CMBlockBufferRef blockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer);
    if (!blockBuffer) {
        return;
    }
    
    size_t totalLength;
    char *dataPointer;
    OSStatus result = CMBlockBufferGetDataPointer(blockBuffer, 0, NULL, &totalLength, &dataPointer);
    
    if (result == noErr && totalLength > 0) {
        NSData *h264Data = [NSData dataWithBytes:dataPointer length:totalLength];
        
        // Send to network manager via callback
        if (manager.onVideoFrame) {
            dispatch_async(dispatch_get_main_queue(), ^{
                manager.onVideoFrame(h264Data);
            });
        }
    }
}

@end