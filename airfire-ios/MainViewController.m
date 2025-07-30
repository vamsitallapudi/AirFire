//
//  MainViewController.m
//  AirFire iOS Broadcaster
//
//  Created on 2025-01-30
//  Based on Moonlight iOS codebase
//

#import "MainViewController.h"

@interface MainViewController ()
@property (nonatomic) BOOL isStreaming;
@end

@implementation MainViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    // Initialize managers
    self.captureManager = [[ScreenCaptureManager alloc] init];
    self.networkManager = [[NetworkManager alloc] init];
    
    // Set default Fire TV IP from successful Android test
    self.fireTVIPField.text = @"192.168.1.21";
    self.fireTVIPLabel.text = @"Fire TV IP: 192.168.1.21:5000";
    
    // Initial UI state
    self.isStreaming = NO;
    [self updateStatus:@"Ready to connect to Fire TV"];
    [self updateUI];
    
    // Setup capture manager delegate
    __weak typeof(self) weakSelf = self;
    self.captureManager.onVideoFrame = ^(NSData *frameData) {
        [weakSelf.networkManager sendVideoFrame:frameData];
    };
    
    self.captureManager.onError = ^(NSError *error) {
        [weakSelf updateStatus:[NSString stringWithFormat:@"Capture Error: %@", error.localizedDescription]];
        [weakSelf stopStreaming:nil];
    };
    
    // Setup network manager callbacks
    self.networkManager.onConnectionStatusChanged = ^(BOOL connected) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [weakSelf updateConnectionStatus:connected];
        });
    };
    
    self.networkManager.onError = ^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [weakSelf updateStatus:[NSString stringWithFormat:@"Network Error: %@", error.localizedDescription]];
            [weakSelf stopStreaming:nil];
        });
    };
}

- (IBAction)startStreaming:(id)sender {
    if (self.isStreaming) {
        return;
    }
    
    NSString *fireTVIP = self.fireTVIPField.text;
    if (!fireTVIP || fireTVIP.length == 0) {
        [self updateStatus:@"Please enter Fire TV IP address"];
        return;
    }
    
    [self updateStatus:@"Connecting to Fire TV..."];
    
    // Connect to Fire TV
    [self.networkManager connectToFireTV:fireTVIP port:5000 completion:^(BOOL success, NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                [self updateStatus:@"Connected! Starting screen capture..."];
                
                // Start screen capture
                [self.captureManager startScreenCapture:^(BOOL success, NSError *error) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        if (success) {
                            self.isStreaming = YES;
                            [self updateStatus:@"🔥 Streaming to Fire TV"];
                            [self updateUI];
                        } else {
                            [self updateStatus:[NSString stringWithFormat:@"Screen capture failed: %@", error.localizedDescription]];
                        }
                    });
                }];
            } else {
                [self updateStatus:[NSString stringWithFormat:@"Connection failed: %@", error.localizedDescription]];
            }
        });
    }];
}

- (IBAction)stopStreaming:(id)sender {
    if (!self.isStreaming) {
        return;
    }
    
    [self updateStatus:@"Stopping stream..."];
    
    // Stop screen capture
    [self.captureManager stopScreenCapture];
    
    // Disconnect from Fire TV
    [self.networkManager disconnect];
    
    self.isStreaming = NO;
    [self updateStatus:@"Stream stopped"];
    [self updateUI];
}

- (void)updateStatus:(NSString *)status {
    self.statusLabel.text = status;
    NSLog(@"AirFire Status: %@", status);
}

- (void)updateConnectionStatus:(BOOL)connected {
    if (connected) {
        self.fireTVIPLabel.textColor = [UIColor greenColor];
        self.fireTVIPLabel.text = [NSString stringWithFormat:@"✅ Connected to %@:5000", self.fireTVIPField.text];
    } else {
        self.fireTVIPLabel.textColor = [UIColor redColor];  
        self.fireTVIPLabel.text = [NSString stringWithFormat:@"❌ Disconnected from %@:5000", self.fireTVIPField.text];
    }
}

- (void)updateUI {
    self.startStreamingButton.enabled = !self.isStreaming;
    self.stopStreamingButton.enabled = self.isStreaming;
    self.fireTVIPField.enabled = !self.isStreaming;
}

@end