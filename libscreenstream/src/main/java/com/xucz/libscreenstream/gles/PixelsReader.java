package com.xucz.libscreenstream.gles;

import com.xucz.libscreenstream.entity.PixelsBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */

public interface PixelsReader {
    void config(int var1, int var2, int var3);

    int getWidth();

    int getHeight();

    void start();

    void stop();

    void showLog(boolean var1);

    void recycleBuffer();

    void shoot(String var1);

    byte[] get();

    void readPixels(int var1);

    PixelsBuffer getPixelsBuffer();

    boolean enablePBO();

    int currentIndex();
}
