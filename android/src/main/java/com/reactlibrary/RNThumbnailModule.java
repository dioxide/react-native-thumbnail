
package com.reactlibrary;

import android.app.Activity;
import android.app.Application;
import android.view.WindowManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import java.util.UUID;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.provider.MediaStore.Images;

public class RNThumbnailModule extends ReactContextBaseJavaModule {

  /**
   * Constant used to indicate the dimension of mini thumbnail.
   * @hide Only used by media framework and media provider internally.
   */
  public static final int TARGET_SIZE_MINI_THUMBNAIL = 320;

  /**
   * Constant used to indicate the dimension of micro thumbnail.
   * @hide Only used by media framework and media provider internally.
   */
  public static final int TARGET_SIZE_MICRO_THUMBNAIL = 96;
  private final ReactApplicationContext reactContext;

  public RNThumbnailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }



  @Override
  public String getName() {
    return "RNThumbnail";
  }

  @ReactMethod
  public void setKeepScreenOn(Boolean bKeepScreenOn) {
    final Activity activity = getCurrentActivity();
    if (bKeepScreenOn == true) {
      if (activity != null) {
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
        });
      }
    } else if (bKeepScreenOn == false) {
      if (activity != null) {
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
        });
      }
    }
  }

  public static Bitmap createVideoThumbnail(String filePath, int kind) {
    Bitmap bitmap = null;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(filePath);
      bitmap = retriever.getFrameAtTime(0);
    } catch (IllegalArgumentException ex) {
      // Assume this is a corrupt video file
    } catch (RuntimeException ex) {
      // Assume this is a corrupt video file.
    } finally {
      try {
        retriever.release();
      } catch (RuntimeException ex) {
        // Ignore failures while cleaning up.
      }
    }

    if (bitmap == null) return null;

    if (kind == Images.Thumbnails.MINI_KIND) {
      // Scale down the bitmap if it's too large.
      int width = bitmap.getWidth();
      int height = bitmap.getHeight();
      int max = Math.max(width, height);
      if (max > 512) {
        float scale = 512f / max;
        int w = Math.round(scale * width);
        int h = Math.round(scale * height);
        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
      }
    } else if (kind == Images.Thumbnails.MICRO_KIND) {
      bitmap = ThumbnailUtils.extractThumbnail(bitmap,
              TARGET_SIZE_MICRO_THUMBNAIL,
              TARGET_SIZE_MICRO_THUMBNAIL,
              ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
    }
    return bitmap;
  }

  @ReactMethod
  public void get(String filePath,String thumbPath, Promise promise) {
    if(filePath == null) {
      return;
    }
    filePath = filePath.replace("file://","");
    String[] items = filePath.split("/");




    try {

      File tempFile =new File(filePath.trim());
      String fileName0 = tempFile.getName();
      Bitmap image = createVideoThumbnail(filePath, Thumbnails.MINI_KIND);
      String fullPath = thumbPath;
      if(fullPath == null || filePath.length() <=0) {
        fullPath =  Environment.getExternalStorageDirectory().getAbsolutePath();
      }
      File dir = new File(fullPath);
      if (!dir.exists()) {
        dir.mkdirs();
      }

      OutputStream fOut = null;
      // String fileName = "thumb-" + UUID.randomUUID().toString() + ".jpeg";
      String fileName = fileName0 + ".jpeg";
      File file = new File(fullPath, fileName);
      file.createNewFile();
      fOut = new FileOutputStream(file);

      // 100 means no compression, the lower you go, the stronger the compression
      image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
      fOut.flush();
      fOut.close();

      // MediaStore.Images.Media.insertImage(reactContext.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

      WritableMap map = Arguments.createMap();

      map.putString("path", "file://" + fullPath + '/' + fileName);
      map.putDouble("width", image.getWidth());
      map.putDouble("height", image.getHeight());

      promise.resolve(map);

    }
    catch (Exception e) {
      //Log.e("E_RNThumnail_ERROR", e.getMessage());
      promise.reject("E_RNThumnail_ERROR", e.getMessage());
    }
  }
}
