package com.xucz.libscreenstream.helper;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.view.Surface;

import com.xucz.libscreenstream.entity.MediaCodecGLWrapper;
import com.xucz.libscreenstream.entity.OffScreenGLWrapper;
import com.xucz.libscreenstream.entity.ScreenGLWrapper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.tools.GLESTools;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
@TargetApi(18)
public class GLHelper {
    private static final int EGL_RECORDABLE_ANDROID = 12610;
    private static String VERTEXSHADER = "attribute vec4 aPosition;\nattribute vec2 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main(){\n    gl_Position= aPosition;\n    vTextureCoord = aTextureCoord;\n}";
    private static final String VERTEXSHADER_CAMERA2D = "attribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nuniform mat4 uTextureMatrix;\nvarying vec2 vTextureCoord;\nvoid main(){\n    gl_Position= aPosition;\n    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n}";
    private static String FRAGMENTSHADER_CAMERA = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying mediump vec2 vTextureCoord;\nuniform sampler2D uTexture;\nvoid main(){\n    vec4  color = texture2D(uTexture, vTextureCoord);\n    gl_FragColor = color;\n}";
    private static String FRAGMENTSHADER_CAMERA2D = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying mediump vec2 vTextureCoord;\nuniform samplerExternalOES uTexture;\nvoid main(){\n    vec4  color = texture2D(uTexture, vTextureCoord);\n    gl_FragColor = color;\n}";
    private static String FRAGMENTSHADER_2D = "precision mediump float;\nvarying mediump vec2 vTextureCoord;\nuniform sampler2D uTexture;\nvoid main(){\n    vec4  color = texture2D(uTexture, vTextureCoord);\n    gl_FragColor = color;\n}";
    private static short[] drawIndices = new short[]{0, 1, 2, 0, 2, 3};
    private static float[] SquareVertices = new float[]{-1.0F, 1.0F, -1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
    private static float[] CamTextureVertices = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F};
    private static float[] Cam2dTextureVertices = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F};
    private static float[] Cam2dTextureVertices_90 = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F};
    private static float[] Cam2dTextureVertices_180 = new float[]{1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F};
    private static float[] Cam2dTextureVertices_270 = new float[]{1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F};
    private static float[] MediaCodecTextureVertices_Background = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F};
    private static float[] MediaCodecTextureVertices_Front = new float[]{1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    private static float[] ScreenTextureVertices = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F};
    public static int FLOAT_SIZE_BYTES = 4;
    public static int SHORT_SIZE_BYTES = 2;
    public static int COORDS_PER_VERTEX = 2;
    public static int TEXTURE_COORDS_PER_VERTEX = 2;
    private static final Object lock = new Object();

    public GLHelper() {
    }

    public static int glVersion(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return info.reqGlEsVersion;
    }

    public static void initOffScreenGL(OffScreenGLWrapper wapper) {
        initOffScreenGL(wapper, (EGLContext) null);
    }

    public static void initOffScreenGL(OffScreenGLWrapper wapper, EGLContext sharedContext) {
        wapper.eglDisplay = EGL14.eglGetDisplay(0);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            PushLog.e("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        } else {
            int[] versions = new int[2];
            if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
                PushLog.e("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            } else {
                int[] configsCount = new int[1];
                EGLConfig[] configs = new EGLConfig[1];
                int[] configSpec = new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12325, 0, 12326, 0, 12344};
                EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
                if (configsCount[0] <= 0) {
                    PushLog.e("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                } else {
                    wapper.eglConfig = configs[0];
                    int[] surfaceAttribs = new int[]{12375, 1, 12374, 1, 12344};
                    int[] contextSpec = new int[]{12440, 2, 12344};
                    wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, null == sharedContext ? EGL14.EGL_NO_CONTEXT : sharedContext, contextSpec, 0);
                    if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
                        PushLog.e("initOffScreenGL eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                    } else {
                        int[] values = new int[1];
                        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, 12440, values, 0);
                        wapper.eglSurface = EGL14.eglCreatePbufferSurface(wapper.eglDisplay, wapper.eglConfig, surfaceAttribs, 0);
                        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
                            PushLog.e("initOffScreenGL eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                        }

                    }
                }
            }
        }
    }

    public static void initMediaCodecGL(MediaCodecGLWrapper wapper, EGLContext sharedContext, Surface mediaInputSurface) {
        wapper.eglDisplay = EGL14.eglGetDisplay(0);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            PushLog.e("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        } else {
            int[] versions = new int[2];
            if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
                PushLog.e("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            } else {
                int[] configsCount = new int[1];
                EGLConfig[] configs = new EGLConfig[1];
                int[] configSpec = new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12610, 1, 12325, 0, 12326, 0, 12344};
                EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
                if (configsCount[0] <= 0) {
                    PushLog.e("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                } else {
                    wapper.eglConfig = configs[0];
                    int[] surfaceAttribs = new int[]{12344};
                    int[] contextSpec = new int[]{12440, 2, 12344};
                    wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, sharedContext, contextSpec, 0);
                    if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
                        PushLog.e("initMediaCodecGL eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                    } else {
                        int[] values = new int[1];
                        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, 12440, values, 0);
                        wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, mediaInputSurface, surfaceAttribs, 0);
                        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
                            PushLog.e("initMediaCodecGL eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                        }

                    }
                }
            }
        }
    }

    public static void initScreenGL(ScreenGLWrapper wapper, EGLContext sharedContext, Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new IllegalStateException("Input must be either a Surface or SurfaceTexture");
        } else {
            wapper.eglDisplay = EGL14.eglGetDisplay(0);
            if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
                PushLog.e("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            } else {
                int[] versions = new int[2];
                if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
                    PushLog.e("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                } else {
                    int[] configsCount = new int[1];
                    EGLConfig[] configs = new EGLConfig[1];
                    int[] configSpec = new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12325, 0, 12326, 0, 12344};
                    EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
                    if (configsCount[0] <= 0) {
                        PushLog.e("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                    } else {
                        wapper.eglConfig = configs[0];
                        int[] surfaceAttribs = new int[]{12344};
                        int[] contextSpec = new int[]{12440, 2, 12344};
                        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, sharedContext, contextSpec, 0);
                        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
                            PushLog.e("initScreenGL eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                        } else {
                            int[] values = new int[1];
                            EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, 12440, values, 0);
                            wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, surface, surfaceAttribs, 0);
                            if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
                                PushLog.e("initScreenGL eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void makeCurrent(OffScreenGLWrapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            PushLog.e("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }

    }

    public static void makeCurrent(MediaCodecGLWrapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            PushLog.e("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }

    }

    public static void makeCurrent(ScreenGLWrapper wapper) {
        synchronized (lock) {
            if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
                PushLog.e("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }

        }
    }

    public static void eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) {
        synchronized (lock) {
            if (!EGL14.eglSwapBuffers(dpy, surface)) {
                PushLog.e("eglSwapBuffers,failed!");
            }

        }
    }

    public static void createFrameBufferWithDepth(int[] frameBuffer, int[] frameBufferTex, int[] frameBufferDep, int length, int width, int height) {
        GLHelper.createFrameBufferWithDepth(frameBuffer, frameBufferTex, frameBufferDep, length, width, height);
    }

    public static void createCamFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenTextures(1, frameBufferTex, 0);
        GLES20.glBindTexture(3553, frameBufferTex[0]);
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, (Buffer) null);
        GLES20.glTexParameterf(3553, 10240, 9729.0F);
        GLES20.glTexParameterf(3553, 10241, 9729.0F);
        GLES20.glTexParameterf(3553, 10242, 33071.0F);
        GLES20.glTexParameterf(3553, 10243, 33071.0F);
        GLES20.glBindFramebuffer(36160, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, frameBufferTex[0], 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
        GLESTools.checkGlError("createCamFrameBuff");
    }

    public static void enableVertex(int posLoc, int texLoc, FloatBuffer shapeBuffer, FloatBuffer texBuffer) {
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(posLoc, COORDS_PER_VERTEX, 5126, false, COORDS_PER_VERTEX * 4, shapeBuffer);
        GLES20.glVertexAttribPointer(texLoc, TEXTURE_COORDS_PER_VERTEX, 5126, false, TEXTURE_COORDS_PER_VERTEX * 4, texBuffer);
    }

    public static void disableVertex(int posLoc, int texLoc) {
        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
    }

    public static int createCamera2DProgram() {
        return GLESTools.createProgram("attribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nuniform mat4 uTextureMatrix;\nvarying vec2 vTextureCoord;\nvoid main(){\n    gl_Position= aPosition;\n    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n}", FRAGMENTSHADER_CAMERA2D);
    }

    public static int createCameraProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_CAMERA);
    }

    public static int createMediaCodecProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static int createScreenProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static ShortBuffer getDrawIndecesBuffer() {
        ShortBuffer result = ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.length).order(ByteOrder.nativeOrder()).asShortBuffer();
        result.put(drawIndices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getShapeVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * SquareVertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(SquareVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getMediaCodecTextureVerticesBuffer(boolean isFrontCamera) {
        float[] buffer;
        if (isFrontCamera) {
            buffer = MediaCodecTextureVertices_Front;
        } else {
            buffer = MediaCodecTextureVertices_Background;
        }

        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * buffer.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(buffer);
        result.position(0);
        return result;
    }

    public static FloatBuffer getScreenTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * ScreenTextureVertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(ScreenTextureVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getCamera2DTextureVerticesBuffer(int directionFlag, float cropRatio) {
        if (directionFlag == -1) {
            FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
            result.put(CamTextureVertices);
            result.position(0);
            return result;
        } else {
            float[] buffer;
            switch (directionFlag & 240) {
                case 32:
                    buffer = (float[]) Cam2dTextureVertices_90.clone();
                    break;
                case 64:
                    buffer = (float[]) Cam2dTextureVertices_180.clone();
                    break;
                case 128:
                    buffer = (float[]) Cam2dTextureVertices_270.clone();
                    break;
                default:
                    buffer = (float[]) Cam2dTextureVertices.clone();
            }

            if ((directionFlag & 240) != 16 && (directionFlag & 240) != 64) {
                if (cropRatio > 0.0F) {
                    buffer[0] = buffer[0] == 1.0F ? 1.0F - cropRatio : cropRatio;
                    buffer[2] = buffer[2] == 1.0F ? 1.0F - cropRatio : cropRatio;
                    buffer[4] = buffer[4] == 1.0F ? 1.0F - cropRatio : cropRatio;
                    buffer[6] = buffer[6] == 1.0F ? 1.0F - cropRatio : cropRatio;
                } else {
                    buffer[1] = buffer[1] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                    buffer[3] = buffer[3] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                    buffer[5] = buffer[5] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                    buffer[7] = buffer[7] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                }
            } else if (cropRatio > 0.0F) {
                buffer[1] = buffer[1] == 1.0F ? 1.0F - cropRatio : cropRatio;
                buffer[3] = buffer[3] == 1.0F ? 1.0F - cropRatio : cropRatio;
                buffer[5] = buffer[5] == 1.0F ? 1.0F - cropRatio : cropRatio;
                buffer[7] = buffer[7] == 1.0F ? 1.0F - cropRatio : cropRatio;
            } else {
                buffer[0] = buffer[0] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                buffer[2] = buffer[2] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                buffer[4] = buffer[4] == 1.0F ? 1.0F + cropRatio : -cropRatio;
                buffer[6] = buffer[6] == 1.0F ? 1.0F + cropRatio : -cropRatio;
            }

            if ((directionFlag & 1) != 0) {
                buffer[0] = flip(buffer[0]);
                buffer[2] = flip(buffer[2]);
                buffer[4] = flip(buffer[4]);
                buffer[6] = flip(buffer[6]);
            }

            if ((directionFlag & 2) != 0) {
                buffer[1] = flip(buffer[1]);
                buffer[3] = flip(buffer[3]);
                buffer[5] = flip(buffer[5]);
                buffer[7] = flip(buffer[7]);
            }

            FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * buffer.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
            result.put(buffer);
            result.position(0);
            return result;
        }
    }

    public static FloatBuffer getCameraTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(CamTextureVertices);
        result.position(0);
        return result;
    }

    private static float flip(float i) {
        return 1.0F - i;
    }
}
