package com.xucz.libscreenstream.pusher.screenstream;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.xucz.libscreenstream.gles.utils.GlUtil;
import com.xucz.libscreenstream.log.PushLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class MarkTexture {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int TOTAL_COMPONENT_COUNT = 4;
    private static final int STRIDE = 16;
    private static final String BITMAP_VERTEX_SHADER = "uniform mat4 uMVPMatrix;\nattribute vec4 aPosition;\nattribute vec2 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = aPosition;\n  vTextureCoord = aTextureCoord;\n}\n";
    private static final String BITMAP_FRAGMENT_SHADER = "precision lowp float;\nvarying vec2 vTextureCoord;\nuniform sampler2D sTexture;\nvoid main()\n{\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}";
    private float aspectRatio = 1.0F;
    private int width;
    private int height;
    private int bitmapTexture = -1;
    private FloatBuffer bitmapVertex;
    private int bitmapProgram;
    private int aBitmapPosition;
    private int aBitmapTextureCoord;
    private int uBitmapTexture;
    private boolean mIsInitialized = false;

    public static MarkTexture create(Bitmap bitmap, int viewWidth, int viewHeight) {
        return new MarkTexture(bitmap, viewWidth, viewHeight);
    }

    public MarkTexture(Bitmap bitmap, int viewWidth, int viewHeight) {
        if (null != bitmap && !bitmap.isRecycled()) {
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
            this.vertical(viewWidth, viewHeight);

            try {
                this.initProgram();
                this.initTexture(bitmap);
                this.mIsInitialized = true;
            } catch (Exception var5) {
                this.mIsInitialized = false;
                var5.printStackTrace();
            }

        } else {
            PushLog.e("initBitmapTexture failed,bitmap is null or recycled");
        }
    }

    private void initVertex(float[] vertex) {
        this.bitmapVertex = ByteBuffer.allocateDirect(vertex.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.bitmapVertex.put(vertex).position(0);
    }

    private void initProgram() {
        this.bitmapProgram = GlUtil.createProgram("uniform mat4 uMVPMatrix;\nattribute vec4 aPosition;\nattribute vec2 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = aPosition;\n  vTextureCoord = aTextureCoord;\n}\n", "precision lowp float;\nvarying vec2 vTextureCoord;\nuniform sampler2D sTexture;\nvoid main()\n{\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}");
        this.aBitmapPosition = GLES20.glGetAttribLocation(this.bitmapProgram, "aPosition");
        this.aBitmapTextureCoord = GLES20.glGetAttribLocation(this.bitmapProgram, "aTextureCoord");
        this.uBitmapTexture = GLES20.glGetUniformLocation(this.bitmapProgram, "sTexture");
    }

    private void initTexture(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES20.glGenTextures(textures.length, textures, 0);
        this.bitmapTexture = textures[0];
        GLES20.glBindTexture(3553, this.bitmapTexture);
        GLES20.glTexParameteri(3553, 10241, 9728);
        GLES20.glTexParameteri(3553, 10240, 9729);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        GLES20.glBindTexture(3553, 0);
        PushLog.e("initBitmapTexture(" + this.bitmapTexture + "), " + bitmap.getWidth() + ", " + bitmap.getHeight());
        bitmap.recycle();
    }

    public void vertical(int viewWidth, int viewHeight) {
        PushLog.e(String.format("vertical(%d, %d)", viewWidth, viewHeight));
        this.aspectRatio = (float) viewWidth / (float) this.width / ((float) viewHeight / (float) this.height);
        this.initVertex(this.getVertexVertical());
    }

    public void horizontal(int viewWidth, int viewHeight) {
        PushLog.e(String.format("horizontal(%d, %d)", viewWidth, viewHeight));
        this.aspectRatio = (float) viewHeight / (float) this.height / ((float) viewWidth / (float) this.width);
        this.initVertex(this.getVertexHorizontal());
    }

    public void draw() {
        if (this.mIsInitialized) {
            if (this.bitmapTexture < 0) {
                PushLog.e("bitmapTexture is invalid");
            } else {
                GLES20.glBindTexture(3553, this.bitmapTexture);
                GLES20.glUseProgram(this.bitmapProgram);
                GLES20.glEnable(3042);
                GLES20.glBlendFunc(770, 771);
                GLES20.glEnableVertexAttribArray(this.aBitmapPosition);
                GLES20.glEnableVertexAttribArray(this.aBitmapTextureCoord);
                GLES20.glUniform1i(this.uBitmapTexture, 0);
                this.bitmapVertex.position(0);
                GLES20.glVertexAttribPointer(this.aBitmapPosition, 2, 5126, false, 16, this.bitmapVertex);
                this.bitmapVertex.position(2);
                GLES20.glVertexAttribPointer(this.aBitmapTextureCoord, 2, 5126, false, 16, this.bitmapVertex);
                GLES20.glDrawArrays(5, 0, 4);
                GLES20.glDisableVertexAttribArray(this.aBitmapPosition);
                GLES20.glDisableVertexAttribArray(this.aBitmapTextureCoord);
                GLES20.glBindTexture(3553, 0);
                GLES20.glUseProgram(0);
            }
        }
    }

    private float[] getVertexVertical() {
        return new float[]{-1.0F, -this.aspectRatio, 0.0F, 1.0F, -1.0F, this.aspectRatio, 0.0F, 0.0F, 1.0F, -this.aspectRatio, 1.0F, 1.0F, 1.0F, this.aspectRatio, 1.0F, 0.0F};
    }

    private float[] getVertexHorizontal() {
        return new float[]{-this.aspectRatio, -1.0F, 0.0F, 1.0F, -this.aspectRatio, 1.0F, 0.0F, 0.0F, this.aspectRatio, -1.0F, 1.0F, 1.0F, this.aspectRatio, 1.0F, 1.0F, 0.0F};
    }

    public void release() {
        GLES20.glDeleteTextures(1, new int[]{this.bitmapTexture}, 0);
        GLES20.glDeleteProgram(this.bitmapProgram);
    }
}
