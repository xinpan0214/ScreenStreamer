package com.xucz.libscreenstream.record_helper;

import android.content.Context;
import android.media.projection.MediaProjectionManager;

/**
 * Description:录屏所需的参数
 *
 * @author 杜乾, Created on 2018/5/10 - 18:31.
 * E-mail:duqian2010@gmail.com
 */
public class PrepareInfo {
    public int screenWidth;
    public int screenHeight;
    public int screenDpi;
    public String savePath;//mp4保存文件
    public boolean isLandscape;//横竖屏

    public PrepareInfo(int screenWidth, int screenHeight, int screenDpi, String savePath, boolean isLandscape) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenDpi = screenDpi;
        this.savePath = savePath;
        this.isLandscape = isLandscape;
    }

    public MediaProjectionManager mMediaProjectionManager;
    public Context context;
}
