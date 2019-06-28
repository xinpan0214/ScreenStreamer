package com.xucz.libscreenstream.gles.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class GlUtil {
    private static final int PBO_SUPPORT_VERSION = 196608;
    public static final int NO_TEXTURE = -1;

    public GlUtil() {
    }

    public static native void glReadPixels(int var0, int var1, int var2, int var3, int var4, int var5);

    public static native void glMapBufferRange(int var0, int var1, byte[] var2);

    /**
     * @deprecated
     */
    @Deprecated
    public static native String glVersion();

    public static int[] getMaxTextureSize() {
        int[] size = new int[1];
        GLES20.glGetIntegerv(3379, size, 0);
        return size;
    }

    public static boolean supportPBO(int glVersion) {
        return glVersion >= 196608;
    }

    public static final float[] identityMatrix() {
        return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    }

    public static final float[] verticalFlipMatrix() {
        return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F};
    }

    public static final float[] horizontalFlipMatrix() {
        return new float[]{-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F};
    }

    public static void checkNoGLES2Error(String msg) {
        int error = GLES20.glGetError();
        if (error != 0) {
            throw new RuntimeException(msg + ": GLES20 error: " + error);
        }
    }

    public static void checkError(String msg) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e("GLES2", msg + ": GLES20 error: " + error);
        }

    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static int generateTexture(int target) {
        int[] textureArray = new int[1];
        GLES20.glGenTextures(1, textureArray, 0);
        int textureId = textureArray[0];
        GLES20.glBindTexture(target, textureId);
        GLES20.glTexParameterf(target, 10241, 9729.0F);
        GLES20.glTexParameterf(target, 10240, 9729.0F);
        GLES20.glTexParameterf(target, 10242, 33071.0F);
        GLES20.glTexParameterf(target, 10243, 33071.0F);
        checkNoGLES2Error("generateTexture");
        return textureId;
    }

    public static void deleteTexture(int texture) {
        if (texture > 0) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
        }

    }

    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        if (vertexShaderCode != null && fragmentShaderCode != null) {
            int vertexShader = GLES20.glCreateShader(35633);
            int fragmentShader = GLES20.glCreateShader(35632);
            GLES20.glShaderSource(vertexShader, vertexShaderCode);
            GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
            int[] status = new int[1];
            GLES20.glCompileShader(vertexShader);
            GLES20.glGetShaderiv(vertexShader, 35713, status, 0);
            if (0 == status[0]) {
                throw new RuntimeException("vertext shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader));
            } else {
                GLES20.glCompileShader(fragmentShader);
                GLES20.glGetShaderiv(fragmentShader, 35713, status, 0);
                if (0 == status[0]) {
                    throw new RuntimeException("fragment shader compile,failed:" + GLES20.glGetShaderInfoLog(fragmentShader));
                } else {
                    int program = GLES20.glCreateProgram();
                    GLES20.glAttachShader(program, vertexShader);
                    GLES20.glAttachShader(program, fragmentShader);
                    GLES20.glLinkProgram(program);
                    GLES20.glGetProgramiv(program, 35714, status, 0);
                    if (0 == status[0]) {
                        throw new RuntimeException("link program,failed:" + GLES20.glGetProgramInfoLog(program));
                    } else {
                        return program;
                    }
                }
            }
        } else {
            throw new RuntimeException("invalid shader code");
        }
    }

    public static void createFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        checkNoGLES2Error("createCamFrameBuff 1");
        GLES20.glGenTextures(1, frameBufferTex, 0);
        checkNoGLES2Error("createCamFrameBuff 2");
        GLES20.glBindTexture(3553, frameBufferTex[0]);
        checkNoGLES2Error("createCamFrameBuff 3");
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, (Buffer) null);
        checkNoGLES2Error("createCamFrameBuff 4");
        GLES20.glTexParameterf(3553, 10240, 9729.0F);
        GLES20.glTexParameterf(3553, 10241, 9729.0F);
        GLES20.glTexParameterf(3553, 10242, 33071.0F);
        GLES20.glTexParameterf(3553, 10243, 33071.0F);
        GLES20.glBindFramebuffer(36160, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, frameBufferTex[0], 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
        checkNoGLES2Error("createCamFrameBuff 5");
    }

    public static String readRawResString(Resources res, int resId) {
        InputStream inputStream = res.openRawResource(resId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();

        String line;
        try {
            while ((line = br.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
        } catch (Exception var7) {
            var7.printStackTrace();
            return null;
        }

        return result.toString();
    }

    public static int loadTexture(Bitmap image, int reUseTexture) {
        int[] texture = new int[1];
        if (image != null && !image.isRecycled()) {
            if (reUseTexture == -1) {
                GLES20.glGenTextures(1, texture, 0);
                GLES20.glBindTexture(3553, texture[0]);
                GLES20.glTexParameterf(3553, 10240, 9729.0F);
                GLES20.glTexParameterf(3553, 10241, 9729.0F);
                GLES20.glTexParameterf(3553, 10242, 33071.0F);
                GLES20.glTexParameterf(3553, 10243, 33071.0F);
                GLUtils.texImage2D(3553, 0, image, 0);
                GLES20.glBindTexture(3553, 0);
            } else {
                GLES20.glBindTexture(3553, reUseTexture);
                GLUtils.texSubImage2D(3553, 0, 0, 0, image);
                texture[0] = reUseTexture;
            }
        }

        return texture[0];
    }

    static {
        System.loadLibrary("glutil");
    }
}
