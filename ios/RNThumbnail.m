
#import "RNThumbnail.h"
#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAsset.h>
#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>

@implementation RNThumbnail

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()


//convert mov file to mp4
RCT_EXPORT_METHOD(convert:(NSString *)movPath resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    @try {
        movPath = [movPath stringByReplacingOccurrencesOfString:@"file://"
                                                     withString:@""];
NSLog(@"videopath of your mov file = %@",movPath);
        AVURLAsset *avAsset = [AVURLAsset URLAssetWithURL:[NSURL fileURLWithPath:movPath] options:nil];
        NSArray *compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:avAsset];

        if ([compatiblePresets containsObject:AVAssetExportPresetLowQuality])
        {
            AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]initWithAsset:avAsset presetName:AVAssetExportPresetPassthrough];
            // save to temp directory

            NSString* tempDirectory = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory,NSUserDomainMask,YES) lastObject];
            NSLog(@"stating convert %@",tempDirectory);
            NSString *videoPath = [tempDirectory stringByAppendingPathComponent: [NSString stringWithFormat:@"%@.mp4", [[NSProcessInfo processInfo] globallyUniqueString]]];

            exportSession.outputURL = [NSURL fileURLWithPath:videoPath];
            NSLog(@"videopath of your mp4 file = %@",videoPath);  // PATH OF YOUR .mp4 FILE
            exportSession.outputFileType = AVFileTypeMPEG4;

            [exportSession exportAsynchronouslyWithCompletionHandler:^{

                switch ([exportSession status]) {

                    case AVAssetExportSessionStatusFailed:
                        reject([[exportSession error] localizedDescription], nil, nil);
                        break;

                    case AVAssetExportSessionStatusCancelled:
                        reject(@"Canceled", nil, nil);
                        break;

                    case AVAssetExportSessionStatusCompleted:
                        resolve(@{ @"path": videoPath });
                        break;

                    default:
                        break;
                }
            }];

        }
    } @catch(NSException *e) {
        reject(e.reason, nil, nil);
    }
}



//按比例缩放,size 是你要把图显示到 多大区域 CGSizeMake(300, 140)
-(UIImage *) imageCompressForSize:(UIImage *)sourceImage targetSize:(CGSize)size{
    UIImage *newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = size.width;
    CGFloat targetHeight = size.height;
    CGFloat scaleFactor = 0.0;
    CGFloat scaledWidth = targetWidth;
    CGFloat scaledHeight = targetHeight;
    CGPoint thumbnailPoint = CGPointMake(0.0, 0.0);
    if(CGSizeEqualToSize(imageSize, size) == NO){
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;
        if(widthFactor > heightFactor){
            scaleFactor = widthFactor;
        }
        else{
            scaleFactor = heightFactor;
        }
        scaledWidth = width * scaleFactor;
        scaledHeight = height * scaleFactor;
        if(widthFactor > heightFactor){
            thumbnailPoint.y = (targetHeight - scaledHeight) * 0.5;
        }else if(widthFactor < heightFactor){
            thumbnailPoint.x = (targetWidth - scaledWidth) * 0.5;
        }
    }
    UIGraphicsBeginImageContext(size);
    CGRect thumbnailRect = CGRectZero;
    thumbnailRect.origin = thumbnailPoint;
    thumbnailRect.size.width = scaledWidth;
    thumbnailRect.size.height = scaledHeight;
    [sourceImage drawInRect:thumbnailRect];
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if(newImage == nil){
        NSLog(@"scale image fail");
    }
    UIGraphicsEndImageContext();
    return newImage;
}

//指定宽度按比例缩放
-(UIImage *) imageCompressForWidth:(UIImage *)sourceImage targetWidth:(CGFloat)defineWidth{
    UIImage *newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = defineWidth;
    CGFloat targetHeight = height / (width / targetWidth);
    CGSize size = CGSizeMake(targetWidth, targetHeight);
    CGFloat scaleFactor = 0.0;
    CGFloat scaledWidth = targetWidth;
    CGFloat scaledHeight = targetHeight;
    CGPoint thumbnailPoint = CGPointMake(0.0, 0.0);
    if(CGSizeEqualToSize(imageSize, size) == NO){
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;
        if(widthFactor > heightFactor){
            scaleFactor = widthFactor;
        }
        else{
            scaleFactor = heightFactor;
        }
        scaledWidth = width * scaleFactor;
        scaledHeight = height * scaleFactor;
        if(widthFactor > heightFactor){
            thumbnailPoint.y = (targetHeight - scaledHeight) * 0.5;
        }else if(widthFactor < heightFactor){
            thumbnailPoint.x = (targetWidth - scaledWidth) * 0.5;
        }
    }
    UIGraphicsBeginImageContext(size);
    CGRect thumbnailRect = CGRectZero;
    thumbnailRect.origin = thumbnailPoint;
    thumbnailRect.size.width = scaledWidth;
    thumbnailRect.size.height = scaledHeight;
    [sourceImage drawInRect:thumbnailRect];
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if(newImage == nil){
        NSLog(@"scale image fail");
    }
    UIGraphicsEndImageContext();
    return newImage;
}


RCT_EXPORT_METHOD(setKeepScreenOn:(BOOL)screenShouldBeKeptOn)
{
    [[UIApplication sharedApplication] setIdleTimerDisabled:screenShouldBeKeptOn];
}


RCT_EXPORT_METHOD(get:(NSString *)filepath
                               thumbPath:(NSString*)thumbPath
                               resolve:(RCTPromiseResolveBlock)resolve
                               reject:(RCTPromiseRejectBlock)reject)
{
    @try {
        filepath = [filepath stringByReplacingOccurrencesOfString:@"file://"
                                                  withString:@""];
        NSURL *vidURL = [NSURL fileURLWithPath:filepath];
        NSLog(@"video outputPath = %@",filepath);
        AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:vidURL options:nil];
        AVAssetImageGenerator *generator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
        generator.appliesPreferredTrackTransform = YES;

        NSError *err = NULL;
        CMTime time = CMTimeMake(1, 60);

        CGImageRef imgRef = [generator copyCGImageAtTime:time actualTime:NULL error:&err];
        UIImage *thumbnail = [UIImage imageWithCGImage:imgRef];


        UIImage* newImage = [self imageCompressForWidth:thumbnail targetWidth:800.0];//fixed width


        NSString* scheme = thumbPath;
        if(scheme == nil) {
            scheme = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,NSUserDomainMask,YES) lastObject];
//NSLog(@"video savepath = %@",scheme);
        }
        // save to temp directory

        NSString *fileName = [vidURL lastPathComponent];
//NSLog(@"video fileName = %@",fileName);
        NSData *data = UIImageJPEGRepresentation(newImage, 1.0);
        NSFileManager *fileManager = [NSFileManager defaultManager];
        NSString *fullPath = [scheme stringByAppendingPathComponent: [NSString stringWithFormat:@"%@.jpeg", fileName]];
//NSLog(@"video outputfile = %@",fullPath);
        [fileManager createFileAtPath:fullPath contents:data attributes:nil];
        if (resolve)
            resolve(@{ @"path" : fullPath,
                       @"width" : [NSNumber numberWithFloat: newImage.size.width],
                       @"height" : [NSNumber numberWithFloat: newImage.size.height] });
    } @catch(NSException *e) {
    NSLog(@"video err = %@",e);
        reject(e.reason, nil, nil);
    }
}

@end

