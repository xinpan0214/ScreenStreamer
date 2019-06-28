package com.xucz.libscreenstream.gles.egl;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLContext;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public abstract class EglBase {
    public static final Object lock = new Object();
    public static final int EGL_OPENGL_ES2_BIT = 4;
    public static final int EGL_RECORDABLE_ANDROID = 12610;
    public static final int[] CONFIG_PLAIN = new int[]{12324, 8, 12323, 8, 12322, 8, 12352, 4, 12344};
    public static final int[] CONFIG_RGBA = new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, 12344};
    public static final int[] CONFIG_PIXEL_BUFFER = new int[]{12324, 8, 12323, 8, 12322, 8, 12352, 4, 12339, 1, 12344};
    public static final int[] CONFIG_PIXEL_RGBA_BUFFER = new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, 12339, 1, 12344};
    public static final int[] CONFIG_RECORDABLE = new int[]{12324, 8, 12323, 8, 12322, 8, 12352, 4, 12610, 1, 12344};
    public static int eglVersion = 10;

    public EglBase() {
    }

    public static EglBase create(EglBase.Context sharedContext, int[] configAttributes) {
        if (!EglBase14.isEGL14Supported() || sharedContext != null && !(sharedContext instanceof EglBase14.Context)) {
            eglVersion = 10;
            return new EglBase10((EglBase10.Context) sharedContext, configAttributes);
        } else {
            eglVersion = 14;
            return new EglBase14((EglBase14.Context) sharedContext, configAttributes);
        }
    }

    public static EglBase create() {
        return create((EglBase.Context) null, CONFIG_PLAIN);
    }

    public static EglBase create(EglBase.Context sharedContext) {
        return create(sharedContext, CONFIG_PLAIN);
    }

    public static EglBase createEgl10(int[] configAttributes) {
        return new EglBase10((EglBase10.Context) null, configAttributes);
    }

    public static EglBase createEgl10(EGLContext sharedContext, int[] configAttributes) {
        return new EglBase10(new EglBase10.Context(sharedContext), configAttributes);
    }

    public static EglBase createEgl14(int[] configAttributes) {
        return new EglBase14((EglBase14.Context) null, configAttributes);
    }

    public static EglBase createEgl14(android.opengl.EGLContext sharedContext, int[] configAttributes) {
        return new EglBase14(new EglBase14.Context(sharedContext), configAttributes);
    }

    public abstract void createSurface(Surface var1);

    public abstract void createSurface(SurfaceTexture var1);

    public abstract void createDummyPbufferSurface();

    public abstract void createPbufferSurface(int var1, int var2);

    public abstract EglBase.Context getEglBaseContext();

    public abstract boolean hasSurface();

    public abstract int surfaceWidth();

    public abstract int surfaceHeight();

    public abstract void releaseSurface();

    public abstract void release();

    public abstract void makeCurrent();

    public abstract void detachCurrent();

    public abstract void swapBuffers();

    public abstract void swapBuffers(long var1);

    public interface Context {
        long getNativeEglContext();

        Object getEGLContext();
    }
}
