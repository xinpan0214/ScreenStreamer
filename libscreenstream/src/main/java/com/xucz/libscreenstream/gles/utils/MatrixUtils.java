package com.xucz.libscreenstream.gles.utils;

import android.opengl.Matrix;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class MatrixUtils {
    public static final int TYPE_FITXY = 0;
    public static final int TYPE_CENTERCROP = 1;
    public static final int TYPE_CENTERINSIDE = 2;
    public static final int TYPE_FITSTART = 3;
    public static final int TYPE_FITEND = 4;

    public MatrixUtils() {
    }

    public static void getMatrix(float[] matrix, int type, int imgWidth, int imgHeight, int viewWidth, int viewHeight) {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == 0) {
                Matrix.orthoM(projection, 0, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, 3.0F);
            }

            float viewAspectRatio = (float) viewWidth / (float) viewHeight;
            float imgAspectRatio = (float) imgWidth / (float) imgHeight;
            if (imgAspectRatio > viewAspectRatio) {
                switch (type) {
                    case 1:
                        Matrix.orthoM(projection, 0, -viewAspectRatio / imgAspectRatio, viewAspectRatio / imgAspectRatio, -1.0F, 1.0F, 1.0F, 3.0F);
                        break;
                    case 2:
                        Matrix.orthoM(projection, 0, -1.0F, 1.0F, -imgAspectRatio / viewAspectRatio, imgAspectRatio / viewAspectRatio, 1.0F, 3.0F);
                        break;
                    case 3:
                        Matrix.orthoM(projection, 0, -1.0F, 1.0F, 1.0F - 2.0F * imgAspectRatio / viewAspectRatio, 1.0F, 1.0F, 3.0F);
                        break;
                    case 4:
                        Matrix.orthoM(projection, 0, -1.0F, 1.0F, -1.0F, 2.0F * imgAspectRatio / viewAspectRatio - 1.0F, 1.0F, 3.0F);
                }
            } else {
                switch (type) {
                    case 1:
                        Matrix.orthoM(projection, 0, -1.0F, 1.0F, -imgAspectRatio / viewAspectRatio, imgAspectRatio / viewAspectRatio, 1.0F, 3.0F);
                        break;
                    case 2:
                        Matrix.orthoM(projection, 0, -viewAspectRatio / imgAspectRatio, viewAspectRatio / imgAspectRatio, -1.0F, 1.0F, 1.0F, 3.0F);
                        break;
                    case 3:
                        Matrix.orthoM(projection, 0, -1.0F, 2.0F * viewAspectRatio / imgAspectRatio - 1.0F, -1.0F, 1.0F, 1.0F, 3.0F);
                        break;
                    case 4:
                        Matrix.orthoM(projection, 0, 1.0F - 2.0F * viewAspectRatio / imgAspectRatio, 1.0F, -1.0F, 1.0F, 1.0F, 3.0F);
                }
            }

            Matrix.setLookAtM(camera, 0, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }

    }

    public static float[] rotate(float[] m, float angle) {
        Matrix.rotateM(m, 0, angle, 0.0F, 0.0F, 1.0F);
        return m;
    }

    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1.0F : 1.0F, y ? -1.0F : 1.0F, 1.0F);
        }

        return m;
    }

    public static float[] scale(float[] m, float x, float y) {
        Matrix.scaleM(m, 0, x, y, 1.0F);
        return m;
    }

    public static float[] getOriginalMatrix() {
        return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    }
}
