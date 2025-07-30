//
//  MainViewController.h
//  AirFire iOS Broadcaster
//
//  Created on 2025-01-30
//  Based on Moonlight iOS codebase
//

#import <UIKit/UIKit.h>
#import <ReplayKit/ReplayKit.h>
#import "ScreenCaptureManager.h"
#import "NetworkManager.h"

@interface MainViewController : UIViewController

@property (weak, nonatomic) IBOutlet UILabel *statusLabel;
@property (weak, nonatomic) IBOutlet UILabel *fireTVIPLabel;
@property (weak, nonatomic) IBOutlet UIButton *startStreamingButton;
@property (weak, nonatomic) IBOutlet UIButton *stopStreamingButton;
@property (weak, nonatomic) IBOutlet UITextField *fireTVIPField;

@property (strong, nonatomic) ScreenCaptureManager *captureManager;
@property (strong, nonatomic) NetworkManager *networkManager;

- (IBAction)startStreaming:(id)sender;
- (IBAction)stopStreaming:(id)sender;
- (void)updateStatus:(NSString *)status;
- (void)updateConnectionStatus:(BOOL)connected;

@end