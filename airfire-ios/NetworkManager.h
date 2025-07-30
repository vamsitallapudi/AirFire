//
//  NetworkManager.h
//  AirFire iOS Broadcaster  
//
//  Created on 2025-01-30
//  Handles TCP connection to Fire TV
//

#import <Foundation/Foundation.h>

@interface NetworkManager : NSObject

// Connection callbacks
@property (nonatomic, copy) void (^onConnectionStatusChanged)(BOOL connected);
@property (nonatomic, copy) void (^onError)(NSError *error);

// Connection methods
- (void)connectToFireTV:(NSString *)ipAddress port:(int)port completion:(void(^)(BOOL success, NSError *error))completion;
- (void)disconnect;
- (BOOL)isConnected;

// Data transmission
- (BOOL)sendVideoFrame:(NSData *)frameData;
- (BOOL)sendData:(NSData *)data;

// Connection info
@property (nonatomic, readonly) NSString *connectedIP;
@property (nonatomic, readonly) int connectedPort;

@end