//
//  NetworkManager.m
//  AirFire iOS Broadcaster
//
//  Created on 2025-01-30
//  Handles TCP connection to Fire TV
//

#import "NetworkManager.h"
#import <sys/socket.h>
#import <netinet/in.h>
#import <arpa/inet.h>

@interface NetworkManager ()
@property (nonatomic) int socketFD;
@property (nonatomic, strong) dispatch_queue_t networkQueue;
@property (nonatomic) BOOL connected;
@property (nonatomic, strong) NSString *connectedIP;
@property (nonatomic) int connectedPort;
@end

@implementation NetworkManager

- (instancetype)init {
    self = [super init];
    if (self) {
        self.socketFD = -1;
        self.connected = NO;
        self.networkQueue = dispatch_queue_create("com.airfire.network", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (void)dealloc {
    [self disconnect];
}

#pragma mark - Public Methods

- (void)connectToFireTV:(NSString *)ipAddress port:(int)port completion:(void(^)(BOOL success, NSError *error))completion {
    if (self.connected) {
        completion(NO, [NSError errorWithDomain:@"AirFireNetworkError" code:2001 userInfo:@{NSLocalizedDescriptionKey: @"Already connected"}]);
        return;
    }
    
    dispatch_async(self.networkQueue, ^{
        BOOL success = [self establishConnection:ipAddress port:port];
        NSError *error = nil;
        
        if (success) {
            self.connected = YES;
            self.connectedIP = ipAddress;
            self.connectedPort = port;
            
            // Notify connection status change
            if (self.onConnectionStatusChanged) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    self.onConnectionStatusChanged(YES);
                });
            }
        } else {
            error = [NSError errorWithDomain:@"AirFireNetworkError" code:2002 userInfo:@{NSLocalizedDescriptionKey: @"Failed to connect to Fire TV"}];
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(success, error);
        });
    });
}

- (void)disconnect {
    dispatch_async(self.networkQueue, ^{
        [self closeConnection];
        
        if (self.connected) {
            self.connected = NO;
            
            // Notify connection status change
            if (self.onConnectionStatusChanged) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    self.onConnectionStatusChanged(NO);
                });
            }
        }
    });
}

- (BOOL)isConnected {
    return self.connected;
}

- (BOOL)sendVideoFrame:(NSData *)frameData {
    if (!self.connected || !frameData || frameData.length == 0) {
        return NO;
    }
    
    // Create frame header with size info
    uint32_t frameSize = (uint32_t)frameData.length;
    uint32_t networkFrameSize = htonl(frameSize); // Convert to network byte order
    
    NSMutableData *packet = [NSMutableData dataWithCapacity:sizeof(uint32_t) + frameData.length];
    [packet appendBytes:&networkFrameSize length:sizeof(uint32_t)];
    [packet appendData:frameData];
    
    return [self sendData:packet];
}

- (BOOL)sendData:(NSData *)data {
    if (!self.connected || self.socketFD == -1) {
        return NO;
    }
    
    __block BOOL success = YES;
    dispatch_sync(self.networkQueue, ^{
        const uint8_t *bytes = (const uint8_t *)data.bytes;
        size_t totalBytes = data.length;
        size_t sentBytes = 0;
        
        while (sentBytes < totalBytes) {
            ssize_t result = send(self.socketFD, bytes + sentBytes, totalBytes - sentBytes, 0);
            
            if (result < 0) {
                // Error occurred
                NSError *error = [NSError errorWithDomain:@"AirFireNetworkError" code:2003 userInfo:@{NSLocalizedDescriptionKey: @"Failed to send data"}];
                [self handleNetworkError:error];
                success = NO;
                break;
            } else if (result == 0) {
                // Connection closed
                NSError *error = [NSError errorWithDomain:@"AirFireNetworkError" code:2004 userInfo:@{NSLocalizedDescriptionKey: @"Connection closed by Fire TV"}];
                [self handleNetworkError:error];
                success = NO;
                break;
            }
            
            sentBytes += result;
        }
    });
    
    return success;
}

#pragma mark - Private Methods

- (BOOL)establishConnection:(NSString *)ipAddress port:(int)port {
    // Create socket
    self.socketFD = socket(AF_INET, SOCK_STREAM, 0);
    if (self.socketFD == -1) {
        NSLog(@"Failed to create socket");
        return NO;
    }
    
    // Configure socket address
    struct sockaddr_in serverAddress;
    memset(&serverAddress, 0, sizeof(serverAddress));
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(port);
    
    if (inet_pton(AF_INET, [ipAddress UTF8String], &serverAddress.sin_addr) <= 0) {
        NSLog(@"Invalid IP address: %@", ipAddress);
        close(self.socketFD);
        self.socketFD = -1;
        return NO;
    }
    
    // Set socket timeout
    struct timeval timeout;
    timeout.tv_sec = 10;  // 10 second timeout
    timeout.tv_usec = 0;
    setsockopt(self.socketFD, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(self.socketFD, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
    
    // Connect to Fire TV
    int result = connect(self.socketFD, (struct sockaddr *)&serverAddress, sizeof(serverAddress));
    if (result == -1) {
        NSLog(@"Failed to connect to %@:%d", ipAddress, port);
        close(self.socketFD);
        self.socketFD = -1;
        return NO;
    }
    
    NSLog(@"Successfully connected to Fire TV at %@:%d", ipAddress, port);
    return YES;
}

- (void)closeConnection {
    if (self.socketFD != -1) {
        close(self.socketFD);
        self.socketFD = -1;
    }
}

- (void)handleNetworkError:(NSError *)error {
    NSLog(@"Network error: %@", error.localizedDescription);
    
    // Close connection
    [self closeConnection];
    self.connected = NO;
    
    // Notify error and connection status change
    if (self.onError) {
        dispatch_async(dispatch_get_main_queue(), ^{
            self.onError(error);
        });
    }
    
    if (self.onConnectionStatusChanged) {
        dispatch_async(dispatch_get_main_queue(), ^{
            self.onConnectionStatusChanged(NO);
        });
    }
}

@end