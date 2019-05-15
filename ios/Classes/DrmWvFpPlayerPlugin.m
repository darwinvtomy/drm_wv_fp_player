#import "DrmWvFpPlayerPlugin.h"
#import <drm_wv_fp_player/drm_wv_fp_player-Swift.h>

@implementation DrmWvFpPlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftDrmWvFpPlayerPlugin registerWithRegistrar:registrar];
}
@end
