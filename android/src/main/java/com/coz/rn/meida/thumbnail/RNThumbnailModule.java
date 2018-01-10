
package com.coz.rn.media.thumbnail;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.provider.MediaStore.Images;
import android.webkit.WebSettings;

import javax.annotation.Nullable;

public class RNThumbnailModule extends ReactContextBaseJavaModule {
  private static final String TAG="RNThumbnailModule";
  private static final String ATTR_ACTION = "action";
  private static final String ATTR_CATEGORY = "category";
  private static final String TAG_EXTRA = "extra";
  private static final String ATTR_DATA = "data";
  private static final String ATTR_TYPE = "type";

  private static final String ATTR_FLAGS = "flags";

  /**
   * Constant used to indicate the dimension of mini thumbnail.
   * @hide Only used by media framework and media provider internally.
   */
  public static final int TARGET_SIZE_MINI_THUMBNAIL = 800;

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

  @ReactMethod
  public void playVideo(ReadableMap params){
    Intent intent = new Intent(Intent.ACTION_VIEW);
    String url = params.getString(ATTR_DATA);

    String type = "video/*";
    Uri uri = Uri.parse(url);

    intent.addCategory("android.intent.category.DEFAULT");
    intent.setDataAndType(uri, type);
    Log.i(TAG,"Intent11:"+intent.toString()+" "+url);

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    Intent chooser = Intent.createChooser(intent, "");
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getReactApplicationContext().startActivity(chooser);
  }

  /**
   * 选用方案
   * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   * getReactApplicationContext().startActivity(intent);
   */
  @ReactMethod
  public void startActivity(ReadableMap params){
    Intent intent = new Intent(Intent.ACTION_VIEW);
    String url = params.getString(ATTR_DATA);




    if (params.hasKey(ATTR_DATA)) {
      intent.setData(Uri.parse(url));

    }
    if (params.hasKey(ATTR_TYPE)) {
      intent.setType(params.getString(ATTR_TYPE));
    }
    if (params.hasKey(TAG_EXTRA)) {
      intent.putExtras(Arguments.toBundle(params.getMap(TAG_EXTRA)));
    }
    if (params.hasKey(ATTR_FLAGS)) {
      intent.addFlags(params.getInt(ATTR_FLAGS));
    }
    if (params.hasKey(ATTR_CATEGORY)) {
      intent.addCategory(params.getString(ATTR_CATEGORY));
    }
    if (params.hasKey(ATTR_ACTION)) {
      intent.setAction(params.getString(ATTR_ACTION));
    } else {
      intent.setAction(Intent.ACTION_VIEW);
    }
    intent.putExtra ("oneshot",0);
    intent.putExtra ("configchange",0);
    Log.i(TAG,"Intent:"+intent.toString()+" "+url);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getReactApplicationContext().startActivity(intent);
    //getReactApplicationContext().startActivityForResult(intent, 0, null); // 暂时使用当前应用的任务栈
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
        fullPath =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/thumb";
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

  private String getCurrentLanguage() {
    Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return current.toLanguageTag();
    } else {
      StringBuilder builder = new StringBuilder();
      builder.append(current.getLanguage());
      if (current.getCountry() != null) {
        builder.append("-");
        builder.append(current.getCountry());
      }
      return builder.toString();
    }
  }

  private String getCurrentCountry() {
    Locale current = getReactApplicationContext().getResources().getConfiguration().locale;
    return current.getCountry();
  }

  private Boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(Build.PRODUCT);
  }

  private Boolean isTablet() {
    int layout = getReactApplicationContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    return layout == Configuration.SCREENLAYOUT_SIZE_LARGE || layout == Configuration.SCREENLAYOUT_SIZE_XLARGE;
  }

  private Boolean is24Hour() {
    return android.text.format.DateFormat.is24HourFormat(this.reactContext.getApplicationContext());
  }


  @Nullable
  @Override
  public Map<String, Object> getConstants() {

    HashMap<String, Object> constants = new HashMap<String, Object>();

    PackageManager packageManager = this.reactContext.getPackageManager();
    String packageName = this.reactContext.getPackageName();

    constants.put("appVersion", "not available");
    constants.put("buildVersion", "not available");
    constants.put("buildNumber", 0);

    try {
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      constants.put("appVersion", info.versionName);
      constants.put("buildNumber", info.versionCode);
      constants.put("firstInstallTime", info.firstInstallTime);
      constants.put("lastUpdateTime", info.lastUpdateTime);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    String deviceName = "Unknown";

    try {
      BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
      if(myDevice!=null){
        deviceName = myDevice.getName();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }


    constants.put("serialNumber", Build.SERIAL);
    constants.put("deviceName", deviceName);
    constants.put("systemName", "Android");
    constants.put("systemVersion", Build.VERSION.RELEASE);
    constants.put("model", Build.MODEL);
    constants.put("brand", Build.BRAND);
    constants.put("deviceId", Build.BOARD);
    constants.put("apiLevel", Build.VERSION.SDK_INT);
    constants.put("deviceLocale", this.getCurrentLanguage());
    constants.put("deviceCountry", this.getCurrentCountry());
    constants.put("uniqueId", Settings.Secure.getString(this.reactContext.getContentResolver(), Settings.Secure.ANDROID_ID));
    constants.put("systemManufacturer", Build.MANUFACTURER);
    constants.put("bundleId", packageName);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      constants.put("userAgent", WebSettings.getDefaultUserAgent(this.reactContext));
    }
    constants.put("timezone", TimeZone.getDefault().getID());
    constants.put("isEmulator", this.isEmulator());
    constants.put("isTablet", this.isTablet());
    constants.put("is24Hour", this.is24Hour());

    return constants;
  }
}
