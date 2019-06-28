package com.xucz.libscreenstream.record_helper;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaMetadataRetriever;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.xucz.libscreenstream.utils.FileUtils;
import com.xucz.libscreenstream.utils.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Description:屏幕截图
 *
 * @author 杜乾, Created on 2018/5/4 - 17:27.
 * E-mail:duqian2010@gmail.com
 */
@TargetApi(21)
public class ScreenshotHelper {

    private static final String TAG = ScreenshotHelper.class.getSimpleName() + " dq-";
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private Handler handler;
    private final String mImagePath;

    private static volatile ScreenshotHelper instance = null;

    public ScreenshotHelper(Context context) {
        screenWidth = SystemUtils.getScreenWidth(context);
        screenHeight = SystemUtils.getScreenHeight(context);
        screenDensity = SystemUtils.getScreenDensity(context);
        handler = new Handler(Looper.getMainLooper());
        mImagePath = RecordStoreManager.getInstance().getLiveScreenshotPath();
    }

    public static ScreenshotHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (ScreenshotHelper.class) {
                if (instance == null) {
                    instance = new ScreenshotHelper(context);
                }
            }
        }
        return instance;
    }

    private VirtualDisplay mVirtualDisplayPic;
    private ImageReader mImageReader;

    /**
     * 屏幕截图
     */
    public synchronized void captureScreenshot(MediaProjection mediaProjection) {
        if (mediaProjection == null) {
            return;
        }
        captureScreenshot(mediaProjection, screenWidth, screenHeight, screenDensity);
    }

    private void captureScreenshot(MediaProjection mediaProjection, int screenWidth, int screenHeight, int screenDensity) {
        if (mediaProjection == null) {
            return;
        }
        mImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplayPic = mediaProjection.createVirtualDisplay("capture_screen_pic", screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
        handler.postDelayed(runnableCapturePic, 1500);//必须延迟，否则因为没有初始化，可能失败
    }

    final Runnable runnableCapturePic = new Runnable() {
        @Override
        public void run() {
            final boolean captured = startCapture(mImagePath, mImageReader);
            if (!captured) {
                FileUtils.deleteFile(mImagePath);
            }
            release();
        }
    };

    public void release() {
        if (mVirtualDisplayPic != null) {
            mVirtualDisplayPic.release();
            mVirtualDisplayPic = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private boolean startCapture(String imagePath, ImageReader imageReader) {
        if (TextUtils.isEmpty(imagePath) || imageReader == null) {
            return false;
        }
        boolean isCaptured = false;
        try {
            Image image = mImageReader.acquireLatestImage();
            if (image == null) {
                return false;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            isCaptured = bitmapToFile(bitmap, imagePath);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            if (mImageReader != null) {
                mImageReader.close();
            }
        }
        return isCaptured;
    }

    /**
     * bitmap转存为本地图片
     *
     * @param bitmap
     * @param imagePath
     * @return
     */
    private static boolean bitmapToFile(Bitmap bitmap, String imagePath) {
        FileUtils.deleteFile(imagePath);
        if (bitmap == null) {
            return false;
        }
        FileOutputStream out = null;
        try {
            File file = new File(imagePath);
            File fileFolder = file.getParentFile();
            if (fileFolder != null && !fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            final boolean compress = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return compress;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 生成缩略图
     *
     * @param videoPath
     * @param targetThumb
     * @return
     */
    public static boolean createVideoThumbnail(String videoPath, String targetThumb) {
        FileUtils.deleteFile(targetThumb);
        final Bitmap bitmap = getVideoThumbnailBitmap(videoPath, 5);
        final boolean bitmapToFile = bitmapToFile(bitmap, targetThumb);
        if (!bitmapToFile) {
            FileUtils.deleteFile(targetThumb);
        }
        return bitmapToFile;
    }

    public static Bitmap getVideoThumbnailBitmap(String filePath, long timeUs) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(timeUs);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

}