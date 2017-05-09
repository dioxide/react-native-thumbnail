
#import "RNThumbnail.h"
#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAsset.h>
#import <UIKit/UIKit.h>

@implementation RNThumbnail

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(get:(NSString *)filepath
                               thumbPath:(NSString*)thumbPath
                               resolve:(RCTPromiseResolveBlock)resolve
                               reject:(RCTPromiseRejectBlock)reject)
{
    @try {
        filepath = [filepath stringByReplacingOccurrencesOfString:@"file://"
                                                  withString:@""];
        NSURL *vidURL = [NSURL fileURLWithPath:filepath];
        
        AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:vidURL options:nil];
        AVAssetImageGenerator *generator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
        generator.appliesPreferredTrackTransform = YES;
        
        NSError *err = NULL;
        CMTime time = CMTimeMake(1, 60);
        
        CGImageRef imgRef = [generator copyCGImageAtTime:time actualTime:NULL error:&err];
        UIImage *thumbnail = [UIImage imageWithCGImage:imgRef];
        
        NSString* scheme = thumbPath;
        if(scheme == nil) {
            scheme = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory,
                                                                           NSUserDomainMask,
                                                                           YES) lastObject];
        }
        // save to temp directory
      
        NSString *fileName = [vidURL lastPathComponent];

        NSData *data = UIImageJPEGRepresentation(thumbnail, 1.0);
        NSFileManager *fileManager = [NSFileManager defaultManager];
        NSString *fullPath = [scheme stringByAppendingPathComponent: [NSString stringWithFormat:@"%@.jpeg", fileName]];
        [fileManager createFileAtPath:fullPath contents:data attributes:nil];
        if (resolve)
            resolve(@{ @"path" : fullPath,
                       @"width" : [NSNumber numberWithFloat: thumbnail.size.width],
                       @"height" : [NSNumber numberWithFloat: thumbnail.size.height] });
    } @catch(NSException *e) {
        reject(e.reason, nil, nil);
    }
}

@end
  
