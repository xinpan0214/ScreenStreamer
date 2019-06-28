package com.xucz.libscreenstream.gles.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class NonoTextureRotationUtil {
    public static final int FLOAT_SIZE_BYTES = 4;
    public static final float[] TEXTURE_NO_ROTATION = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
    public static final float[] TEXTURE_ROTATED_90 = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F};
    public static final float[] TEXTURE_ROTATED_180 = new float[]{1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F};
    public static final float[] TEXTURE_ROTATED_270 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};
    public static final float[] CUBE = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F};

    private NonoTextureRotationUtil() {
    }

    public static float[] getRotation(int rotation, boolean flipHorizontal, boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case 0:
            case 360:
            default:
                rotatedTex = TEXTURE_NO_ROTATION;
                break;
            case 90:
                rotatedTex = TEXTURE_ROTATED_90;
                break;
            case 180:
                rotatedTex = TEXTURE_ROTATED_180;
                break;
            case 270:
                rotatedTex = TEXTURE_ROTATED_270;
        }

        if (flipHorizontal) {
            rotatedTex = new float[]{flip(rotatedTex[0]), rotatedTex[1], flip(rotatedTex[2]), rotatedTex[3], flip(rotatedTex[4]), rotatedTex[5], flip(rotatedTex[6]), rotatedTex[7]};
        }

        if (flipVertical) {
            rotatedTex = new float[]{rotatedTex[0], flip(rotatedTex[1]), rotatedTex[2], flip(rotatedTex[3]), rotatedTex[4], flip(rotatedTex[5]), rotatedTex[6], flip(rotatedTex[7])};
        }

        return rotatedTex;
    }

    public static FloatBuffer getRotationFloatBuffer(int rotation, boolean flipHorizontal, boolean flipVertical) {
        float[] rotatedTex = getRotation(rotation, flipHorizontal, flipVertical);
        FloatBuffer result = ByteBuffer.allocateDirect(4 * rotatedTex.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(rotatedTex);
        result.position(0);
        return result;
    }

    public static FloatBuffer getSquareVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(4 * CUBE.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(CUBE);
        result.position(0);
        return result;
    }

    private static float flip(float i) {
        return i == 0.0F ? 1.0F : 0.0F;
    }
}
