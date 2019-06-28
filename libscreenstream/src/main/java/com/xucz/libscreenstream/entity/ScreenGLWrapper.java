package com.xucz.libscreenstream.entity;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class ScreenGLWrapper {
    public EGLDisplay eglDisplay;
    public EGLConfig eglConfig;
    public EGLSurface eglSurface;
    public EGLContext eglContext;
    public int drawProgram;
    public int drawTextureLoc;
    public int drawPostionLoc;
    public int drawTextureCoordLoc;

    public ScreenGLWrapper() {
    }
}
