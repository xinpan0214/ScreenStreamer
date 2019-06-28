package com.xucz.libscreenstream.callback;

import android.graphics.Bitmap;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public interface IScreenShotListener {
    void onScreenShotResult(Bitmap var1);

    public static class ScreenShotListenerRunnable implements Runnable {
        Bitmap resultBitmap;
        IScreenShotListener resScreenShotListener;

        public ScreenShotListenerRunnable(IScreenShotListener listener, Bitmap bitmap) {
            this.resScreenShotListener = listener;
            this.resultBitmap = bitmap;
        }

        public void run() {
            if (this.resScreenShotListener != null) {
                this.resScreenShotListener.onScreenShotResult(this.resultBitmap);
            }

        }
    }
}
