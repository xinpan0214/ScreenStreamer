package com.xucz.libscreenstream.tools;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.xucz.libscreenstream.log.PushLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class GLESTools {
    public static int FLOAT_SIZE_BYTES = 4;
    public static final int NO_TEXTURE = -1;

    public GLESTools() {
    }

    public static String readTextFile(Resources res, int resId) {
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

    public static int createProgram(Resources res, int vertexShaderResId, int fragmentShaderResId) {
        String vertexShaderCode = readTextFile(res, vertexShaderResId);
        String fragmentShaderCode = readTextFile(res, fragmentShaderResId);
        return createProgram(vertexShaderCode, fragmentShaderCode);
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

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != 0) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            PushLog.e(msg);
        }

    }

    public static int loadTexture(Bitmap image, int reUseTexture) {
        int[] texture = new int[1];
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

        return texture[0];
    }

    public static void createFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenTextures(1, frameBufferTex, 0);
        GLES20.glBindTexture(3553, frameBufferTex[0]);
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, (Buffer) null);
        checkGlError("createCamFrameBuff");
        GLES20.glTexParameterf(3553, 10240, 9729.0F);
        GLES20.glTexParameterf(3553, 10241, 9729.0F);
        GLES20.glTexParameterf(3553, 10242, 33071.0F);
        GLES20.glTexParameterf(3553, 10243, 33071.0F);
        GLES20.glBindFramebuffer(36160, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, frameBufferTex[0], 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
        checkGlError("createCamFrameBuff");
    }
}
