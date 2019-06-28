package com.xucz.libscreenstream.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import com.xucz.libscreenstream.log.PushLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.MEDIA_MOUNTED;

public class StorageUtils {
    public static final String SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String WATER_MARK_PATH = Environment.getExternalStorageDirectory().getPath() + "/.nono/watermark";

    private static final String CONFIG_DIR_NAME = "config-cache";

    public static File getConfigCacheDirectory(Context context) {
        File cacheDir = getCacheDirectory(context, true);
        return new File(cacheDir, CONFIG_DIR_NAME);
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> (if card is mounted and app has appropriate permission) or
     * on device's file system depending incoming parameters.
     *
     * @param context        Application context
     * @param preferExternal Whether prefer external location for cache
     * @return Cache {@link File directory}.<br />
     * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
     * {@link Context#getCacheDir() Context.getCacheDir()} returns null).
     */
    public static File getCacheDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens
            externalStorageState = "";
        }
        if (preferExternal && MEDIA_MOUNTED.equals(externalStorageState)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = context.getFilesDir().getAbsolutePath() + "/cache/";
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                return null;
            }
        }
        return appCacheDir;
    }

    //获取emulate/0/.nono 文件夹
    public static File getExternalNonoDir() {
        File file = new File(Environment.getExternalStorageDirectory(), ".nono");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    //emulate/0/.nono 拍摄存下来的图片在这里
    public static Uri getOutputMediaFileUri(Context context, String authorities) {
        //storage/emulated/0/Pictures/NonoLive
        File mediaStorageDir = new File(getExternalNonoDir(), "NonoLive");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                PushLog.d("NonoLive", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        PushLog.i("photo store path=" + mediaStorageDir.getPath());
        try {
            Uri photoURI = FileProvider.getUriForFile(context, authorities, mediaFile);
            return photoURI;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Uri getOutputFileUri(Context context, String authorities, File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                PushLog.d("NonoLive", "failed to create directory");
                return null;
            }
        }
        try {
            Uri fileURI = FileProvider.getUriForFile(context, authorities, file);
            return fileURI;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
