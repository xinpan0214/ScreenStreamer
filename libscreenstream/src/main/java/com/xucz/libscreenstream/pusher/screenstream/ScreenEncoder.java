package com.xucz.libscreenstream.pusher.screenstream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.Surface;

import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.gles.egl.EglBase;
import com.xucz.libscreenstream.gles.model.CmmScreenGLWrapper;
import com.xucz.libscreenstream.gles.utils.GlUtil;
import com.xucz.libscreenstream.gles.utils.NonoTextureRotationUtil;
import com.xucz.libscreenstream.helper.GLHelper;
import com.xucz.libscreenstream.helper.MediaCodecHelper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;
import com.xucz.libscreenstream.utils.FrameRateMeter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */

@TargetApi(21)
public final class ScreenEncoder {
    private static final int DRAW_FPS = 60;
    private static final int MSG_WHAT_INIT = 1;
    private static final int MSG_WHAT_RELEASE = 2;
    private static final int MSG_WHAT_START_STREAM = 3;
    private static final int MSG_WHAT_STOP_STREAM = 4;
    private static final int MSG_WHAT_UPDATE_BITRATE = 5;
    private static final int MSG_WHAT_FRAME_AVAILABLE = 6;
    private static final int MSG_WHAT_DRAW_FRAME = 8;
    private static final int MSG_WHAT_TOGGLE_VIDEO = 9;
    private static final int MSG_WHAT_MARK = 10;
    private ScreenEncoder.ScreenEncoderHandler mHandler;
    private long oneFrameDuration = 16L;
    private BaseMp4Muxer baseMp4Muxer;

    public ScreenEncoder(Configure configure, BaseMp4Muxer baseMp4Muxer, ScreenEncoder.ScreenEncoderCallback callback) {
        this.baseMp4Muxer = baseMp4Muxer;
        HandlerThread captureThread = new HandlerThread("ScreenCaptureThread");
        captureThread.start();
        this.mHandler = new ScreenEncoder.ScreenEncoderHandler(captureThread.getLooper(), baseMp4Muxer, configure, callback);
    }

    public void init() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void startStream(int orientation) {
        if (this.mHandler.isAlive()) {
            Message message = this.mHandler.obtainMessage(3, orientation, 0);
            this.mHandler.removeMessages(3);
            this.mHandler.sendMessage(message);
            this.mHandler.removeMessages(8);
            this.mHandler.sendDrawFrameMsg(this.oneFrameDuration);
        }
    }

    public void stopStream() {
        if (this.mHandler.isAlive()) {
            this.mHandler.removeMessages(4);
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void changeBitrate(int newBitrate) {
        if (this.mHandler.isAlive() && newBitrate > 0) {
            PushLog.d("ScreenEncoder changeBitrate = " + newBitrate);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(5, newBitrate, 0));
        }
    }

    public void updateScreenOrientation(int orientation) {
        if (this.mHandler.isAlive()) {
            if (this.baseMp4Muxer != null) {
                this.baseMp4Muxer.pause();
            }

            this.stopStream();
            this.startStream(orientation);
        }
    }

    public void toggleVideo(boolean isDisable) {
        if (this.mHandler.isAlive()) {
            this.mHandler.removeMessages(9);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(9, isDisable));
        }
    }

    public void release() {
        if (this.mHandler.isAlive()) {
            this.mHandler.release();
        }
    }

    public boolean isDisableVideo() {
        return this.mHandler.isDisableVideo();
    }

    public void setMarkTexture(Bitmap bitmap) {
        this.toggleVideo(null != bitmap);
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, bitmap));
    }

    public float getVideoCaptureFrameRate() {
        return !this.mHandler.isAlive() ? 0.0F : this.mHandler.getDrawFps();
    }

    public float getScreenCaptureFrameRate() {
        return !this.mHandler.isAlive() ? 0.0F : this.mHandler.getScreenCaptureFps();
    }

    public interface ScreenEncoderCallback {
        int ERROR_INIT = 10;
        int ERROR_GEN_OFFSURFACE = 11;
        int ERROR_INIT_MEDIACODEC = 12;
        int ERROR_CREATE_VIRTUAL_DISPLAY = 13;

        void onSurfaceTexture(SurfaceTexture var1);

        void onError(int var1, String var2);

        void onVideoSequenceHeader(byte[] var1, int var2, byte[] var3, int var4, boolean var5);

        void onVideoPacket(byte[] var1, int var2, int var3, boolean var4);

        void onVideoEncodeError(int var1);
    }

    private static class ScreenEncoderHandler extends Handler {
        private static final int MAX_RETRY_COUNT = 2;
        private static final long ONE_SECOND = 1000L;
        private static final int LIMIT_MAX_FPS = 35;
        private static final int LIMIT_MIN_FPS = 6;
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int COORDS_VERTICES_DATA_STRIDE_BYTES = 8;
        private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;\nuniform mat4 uSTMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = uMVPMatrix * aPosition;\n  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n}\n";
        private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n";
        private static String FRAGMENT_SHADER_SAMPLE2D =
                "precision mediump float;                               \n" +
                        "varying mediump vec2 vTextureCoord;                    \n" +
                        "uniform sampler2D sTexture;                            \n" +
                        "void main(){                                           \n" +
                        "    vec4  color = texture2D(sTexture, vTextureCoord);  \n" +
                        "    gl_FragColor = color;                              \n" +
                        "}";
        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];
        private CmmScreenGLWrapper mOffScreenGLWrapper;
        private SurfaceTexture mSurfaceTexture;
        private int oesTextureId = 0;
        private int sample2DFrameBuffer;
        private int sample2DFrameBufferTexture;
        private float[] mVerticesTransformMatrix = new float[16];
        private FloatBuffer shapeVerticesBuffer = null;
        private FloatBuffer screenCaptureTextureBuffer = null;
        private FloatBuffer mediaCodecTextureBuffer = null;
        private CmmScreenGLWrapper mMediaCodecGLWrapper;
        private MediaCodec hwEncoder = null;
        private MediaFormat videoFormat = null;
        private boolean isAlive;
        private boolean hasNewFrame = false;
        private boolean updateSurface = false;
        private final Object mFrameUpdateLock = new Object();
        private boolean hasCreateFrameBuffers = false;
        private boolean isStreaming = false;
        private ScreenEncoder.ScreenEncoderCallback screenEncoderCallback = null;
        private long oneFrameDuration;
        private long encoderStartTime = 0L;
        private long lastDrawFrameTime = 0L;
        private long lastFrameAvailableTime = 0L;
        private boolean isDisableVideo = false;
        private long limitMaxFpsTime;
        private long limitMinFpsTime;
        private int captureWidth;
        private int captureHeight = 0;
        private int outputWidth;
        private int outputHeight;
        private int videoBitRate;
        private int videoFrameRate;
        private int videoGOPSize = 0;
        private int enableHighMode = 1;
        private int mOrientation = -1;
        private int encodeErrorNum = 0;
        private MarkTexture markTexture;
        private BaseMp4Muxer baseMp4Muxer;
        private FrameRateMeter drawFrameRateMeter;
        private FrameRateMeter screenCaptureFrameRateMeter;
        private long mLastPts;
        private int retryCount = 0;
        private boolean isAsyncMode = false;
        private ScreenDataThread screenDataThread = null;

        public ScreenEncoderHandler(Looper looper, BaseMp4Muxer baseMp4Muxer, Configure configure, ScreenEncoder.ScreenEncoderCallback callback) {
            super(looper);
            this.baseMp4Muxer = baseMp4Muxer;
            int limitMinFps = 6;
            if (configure.videoFPS == 0) {
                configure.videoFPS = 20;
            }

            this.isAlive = true;
            int limitMaxFps = configure.videoFPS + 2;
            this.oneFrameDuration = 1000L / (long) configure.videoFPS;
            this.limitMaxFpsTime = 1000L / (long) limitMaxFps;
            this.limitMinFpsTime = 1000L / (long) limitMinFps;
            this.initStreamAVOptions(configure);
            this.screenEncoderCallback = callback;
            this.initBuffer();
            this.drawFrameRateMeter = new FrameRateMeter();
            this.screenCaptureFrameRateMeter = new FrameRateMeter();
        }

        private void initStreamAVOptions(Configure configure) {
            this.captureWidth = configure.previewVideoWidth;
            this.captureHeight = configure.previewVideoHeight;
            this.outputWidth = configure.videoWidth;
            this.outputHeight = configure.videoHeight;
            this.videoFrameRate = configure.videoFPS;
            this.videoBitRate = configure.videoBitRate;
            this.videoGOPSize = configure.videoFPS * 2;
            this.enableHighMode = configure.enableHighMode;
        }

        private void initBuffer() {
            this.shapeVerticesBuffer = NonoTextureRotationUtil.getSquareVerticesBuffer();
            this.mediaCodecTextureBuffer = NonoTextureRotationUtil.getRotationFloatBuffer(0, false, false);
            Matrix.setIdentityM(this.mSTMatrix, 0);
        }

        private ScreenEncoder.ScreenEncoderCallback getCallback() {
            return this.screenEncoderCallback;
        }

        public boolean isAlive() {
            return this.isAlive;
        }

        public float getDrawFps() {
            return this.drawFrameRateMeter.getFps();
        }

        public float getScreenCaptureFps() {
            return this.screenCaptureFrameRateMeter.getFps();
        }

        private void sendMsgEx(Message msg, int what, long delay) {
            this.removeMessages(what);
            this.sendMessageDelayed(msg, delay);
        }

        public void sendDrawFrameMsg(long interval) {
            if (this.isAlive()) {
                Message drawMsg;
                if (interval > 0L) {
                    drawMsg = this.obtainMessage(8, SystemClock.uptimeMillis() + interval);
                    this.sendMsgEx(drawMsg, 8, interval);
                } else {
                    drawMsg = this.obtainMessage(8, SystemClock.uptimeMillis() + this.oneFrameDuration);
                    this.sendMsgEx(drawMsg, 8, 0L);
                }

            }
        }

        private void updateOESSurfaceTextureTexImage() {
            if (this.isAlive()) {
                synchronized (this.mFrameUpdateLock) {
                    this.sendMsgEx(this.obtainMessage(6), 6, 0L);
                }
            }
        }

        public void release() {
            if (this.isAlive()) {
                this.removeMessages(2);
                this.sendMessageAtFrontOfQueue(this.obtainMessage(2));
            }
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    this.initInner();
                    break;
                case 2:
                    this.releaseInner();
                    break;
                case 3:
                    this.startStreamInner(msg.arg1);
                    break;
                case 4:
                    this.stopStreamInner();
                    break;
                case 5:
                    this.updateEncoderBitrateInner(msg.arg1);
                    break;
                case 6:
                    this.updateOESSurfaceTextureTexImageInner();
                case 7:
                default:
                    break;
                case 8:
                    long time = (Long) msg.obj;
                    this.drawFrameInner(time);
                    break;
                case 9:
                    this.toggleVideoInner((Boolean) msg.obj);
                    break;
                case 10:
                    this.initMarkTexture(null == msg.obj ? null : (Bitmap) msg.obj);
            }

        }

        private void initInner() {
            if (!this.initOffScreenEGL()) {
                if (this.getCallback() != null) {
                    this.getCallback().onError(10, "Fail to init off screen egl!");
                }

            } else if (!this.genOESSurfaceTexture()) {
                if (this.getCallback() != null) {
                    this.getCallback().onError(11, "Fail to create off screen surface!");
                }

            } else {
                try {
                    this.initOffScreenGL();
                } catch (Exception var2) {
                    var2.printStackTrace();
                    if (this.getCallback() != null) {
                        this.getCallback().onError(10, "Fail to init off screen program!");
                    }
                }

            }
        }

        private void releaseInner() {
            this.isAlive = false;
            this.releaseMediaCodecGL();
            this.releaseOffScreenGL();
            this.getLooper().quit();
            PushLog.e("ScreenEncoder release success!");
        }

        private void initMarkTexture(Bitmap bitmap) {
            if (null == bitmap) {
                this.markTexture = null;
            } else {
                this.markTexture = MarkTexture.create(bitmap, this.captureWidth, this.captureHeight);
            }
        }

        private boolean initOffScreenEGL() {
            this.mOffScreenGLWrapper = new CmmScreenGLWrapper();

            try {
                this.mOffScreenGLWrapper.mEglBase = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);
                this.mOffScreenGLWrapper.mEglBase.createDummyPbufferSurface();
                this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                return true;
            } catch (Exception var2) {
                var2.printStackTrace();
                this.release();
                return false;
            }
        }

        private void initOffScreenGL() {
            if (this.mOffScreenGLWrapper != null) {
                this.mOffScreenGLWrapper.mProgram = GlUtil.createProgram("uniform mat4 uMVPMatrix;\nuniform mat4 uSTMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = uMVPMatrix * aPosition;\n  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n}\n", "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n");
                if (this.mOffScreenGLWrapper.mProgram == 0) {
                    throw new RuntimeException("failed creating program");
                }

                this.mOffScreenGLWrapper.maPositionHandle = GLES20.glGetAttribLocation(this.mOffScreenGLWrapper.mProgram, "aPosition");
                GlUtil.checkNoGLES2Error("glGetAttribLocation aPosition");
                if (this.mOffScreenGLWrapper.maPositionHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for aPosition");
                }

                this.mOffScreenGLWrapper.maTextureHandle = GLES20.glGetAttribLocation(this.mOffScreenGLWrapper.mProgram, "aTextureCoord");
                GlUtil.checkNoGLES2Error("glGetAttribLocation aTextureCoord");
                if (this.mOffScreenGLWrapper.maTextureHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for aTextureCoord");
                }

                this.mOffScreenGLWrapper.muMVPMatrixHandle = GLES20.glGetUniformLocation(this.mOffScreenGLWrapper.mProgram, "uMVPMatrix");
                GlUtil.checkNoGLES2Error("glGetUniformLocation uMVPMatrix");
                if (this.mOffScreenGLWrapper.muMVPMatrixHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for uMVPMatrix");
                }

                this.mOffScreenGLWrapper.muSTMatrixHandle = GLES20.glGetUniformLocation(this.mOffScreenGLWrapper.mProgram, "uSTMatrix");
                GlUtil.checkNoGLES2Error("glGetUniformLocation uSTMatrix");
                if (this.mOffScreenGLWrapper.muSTMatrixHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for uSTMatrix");
                }

                this.mOffScreenGLWrapper.musTexture = GLES20.glGetUniformLocation(this.mOffScreenGLWrapper.mProgram, "sTexture");
                GlUtil.checkNoGLES2Error("glGetUniformLocation sTexture");
                if (this.mOffScreenGLWrapper.musTexture == -1) {
                    throw new RuntimeException("Could not get attrib location for sTexture");
                }
            }

        }

        private void releaseOffScreenGL() {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null) {
                try {
                    this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                    if (this.mOffScreenGLWrapper.mProgram != 0) {
                        GLES20.glDeleteProgram(this.mOffScreenGLWrapper.mProgram);
                    }
                } catch (Exception var3) {
                    var3.printStackTrace();
                }

                this.deleteFrameBuffers();
                this.releaseOESSurface();

                try {
                    this.mOffScreenGLWrapper.mEglBase.release();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }

                this.mOffScreenGLWrapper.mEglBase = null;
                this.mOffScreenGLWrapper = null;
            }

        }

        private boolean genOESSurfaceTexture() {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null) {
                try {
                    this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                    this.oesTextureId = GlUtil.generateTexture(36197);
                } catch (Exception var4) {
                    var4.printStackTrace();
                    this.release();
                    return false;
                }

                this.mSurfaceTexture = new SurfaceTexture(this.oesTextureId);
                this.mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        synchronized (ScreenEncoderHandler.this.mFrameUpdateLock) {
                            ScreenEncoderHandler.this.updateSurface = true;
                        }

                        ScreenEncoderHandler.this.updateOESSurfaceTextureTexImage();
                    }
                });
                if (this.getCallback() != null) {
                    this.getCallback().onSurfaceTexture(this.mSurfaceTexture);
                }

                synchronized (this.mFrameUpdateLock) {
                    this.updateSurface = false;
                    return true;
                }
            } else {
                return false;
            }
        }

        private void releaseOESSurface() {
            if (this.mSurfaceTexture != null) {
                this.mSurfaceTexture.setOnFrameAvailableListener((SurfaceTexture.OnFrameAvailableListener) null);
                this.mSurfaceTexture.release();
            }

            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null && this.oesTextureId > 0) {
                try {
                    this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                    GlUtil.deleteTexture(this.oesTextureId);
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }

            this.mSurfaceTexture = null;
            this.oesTextureId = 0;
        }

        private void startStreamInner(int orientation) {
            boolean isNeedSwapReslution = false;
            if (orientation != 90 && orientation != 270) {
                if (this.captureWidth > this.captureHeight || this.outputWidth > this.outputHeight) {
                    isNeedSwapReslution = true;
                }
            } else if (this.captureWidth < this.captureHeight || this.outputWidth < this.outputHeight) {
                isNeedSwapReslution = true;
            }

            if (isNeedSwapReslution) {
                int temp = this.captureWidth;
                this.captureWidth = this.captureHeight;
                this.captureHeight = temp;
                temp = this.outputWidth;
                this.outputWidth = this.outputHeight;
                this.outputHeight = temp;
            }

            float captureRatio = (float) this.captureHeight * 1.0F / (float) this.captureWidth;
            float outputRatio = (float) this.outputHeight * 1.0F / (float) this.outputWidth;
            this.screenCaptureTextureBuffer = NonoTextureRotationUtil.getRotationFloatBuffer(0, false, false);
            if (this.mOrientation != orientation || !this.hasCreateFrameBuffers) {
                this.createFrameBuffers();
            }

            this.calcAdaptMediaCodecMatrix(captureRatio, outputRatio);
            this.initMediaCodec(this.outputWidth, this.outputHeight, this.videoBitRate, this.videoFrameRate, this.videoGOPSize);
            this.isStreaming = true;
            if (orientation != 90 && orientation != 270) {
                if (null != this.markTexture) {
                    this.markTexture.vertical(this.captureWidth, this.captureHeight);
                }
            } else if (null != this.markTexture) {
                this.markTexture.horizontal(this.captureWidth, this.captureHeight);
            }

            this.mOrientation = orientation;
        }

        private void stopStreamInner() {
            PushLog.i("stopStreamInner release hwencoder");
            if (this.screenDataThread != null) {
                this.screenDataThread.quit();

                try {
                    this.screenDataThread.join(100L);
                } catch (InterruptedException var2) {
                    var2.printStackTrace();
                }

                this.screenDataThread = null;
            }

            this.releaseHWEncoder();
            PushLog.i("stopStreamInner release mediacodec gl");
            this.releaseMediaCodecGL();
            if (null != this.markTexture) {
                this.markTexture.release();
            }

            this.isStreaming = false;
        }

        private void updateEncoderBitrateInner(int newBitrate) {
            this.videoBitRate = newBitrate;
            if (this.isStreaming) {
                this.stopStreamInner();
                this.startStreamInner(this.mOrientation);
            }

        }

        private void toggleVideoInner(boolean isDisable) {
            this.isDisableVideo = isDisable;
        }

        public boolean isDisableVideo() {
            return this.isDisableVideo;
        }

        private void createFrameBuffers() {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null) {
                try {
                    this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                } catch (Exception var5) {
                    var5.printStackTrace();
                    this.hasCreateFrameBuffers = false;
                    return;
                }

                if (this.hasCreateFrameBuffers) {
                    this.hasCreateFrameBuffers = false;
                    GLES20.glDeleteFramebuffers(1, new int[]{this.sample2DFrameBuffer}, 0);
                    GLES20.glDeleteTextures(1, new int[]{this.sample2DFrameBufferTexture}, 0);
                }

                int[] fb = new int[1];
                int[] fbt = new int[1];

                try {
                    GLHelper.createCamFrameBuff(fb, fbt, this.captureWidth, this.captureHeight);
                } catch (Exception var4) {
                    var4.printStackTrace();
                    return;
                }

                this.sample2DFrameBuffer = fb[0];
                this.sample2DFrameBufferTexture = fbt[0];
                this.hasCreateFrameBuffers = true;
                this.hasNewFrame = false;
                this.lastDrawFrameTime = 0L;
                PushLog.d("createFrameBuffers===========>>>>");
            }

        }

        private void deleteFrameBuffers() {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null && this.hasCreateFrameBuffers) {
                this.hasCreateFrameBuffers = false;

                try {
                    this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                } catch (Exception var2) {
                    var2.printStackTrace();
                    return;
                }

                GLES20.glDeleteFramebuffers(1, new int[]{this.sample2DFrameBuffer}, 0);
                GLES20.glDeleteTextures(1, new int[]{this.sample2DFrameBufferTexture}, 0);
            }

        }

        private void initMediaCodec(int videoWidth, int videoHeight, int videoBitrate, int videoFrameRate, int videoGopSize) {
            this.mLastPts = 0L;

            try {
                this.hwEncoder = MediaCodec.createEncoderByType("video/avc");
            } catch (Exception var7) {
                var7.printStackTrace();
                this.releaseHWEncoder();
                if (this.getCallback() != null) {
                    this.getCallback().onError(12, "Fail to create MediaCodec");
                }

                return;
            }

            this.videoFormat = MediaCodecHelper.createVideoFormat(videoWidth, videoHeight, videoBitrate, videoFrameRate, videoGopSize, false);
            if (1 == this.enableHighMode) {
                PushLog.e("enableHighMode=" + this.enableHighMode);
                MediaCodecHelper.setProfileLevel((Configure) null, this.hwEncoder, this.videoFormat);
            }

            PushLog.e("mediaFormat=" + this.videoFormat.toString());
            if (!this.isAsyncMode) {
                this.hwEncoder.setCallback(new MediaCodec.Callback() {

                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    }

                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo eInfo) {
                        ScreenEncoderHandler.this.deliverEncodedImage(codec, outputBufferId, eInfo);
                    }

                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        PushLog.e("MediaCodec onError " + e.getMessage());
                    }

                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat mediaFormat) {
                        byte[] sps = ScreenEncoderHandler.this.getSPSByte(mediaFormat);
                        byte[] pps = ScreenEncoderHandler.this.getPPSByte(mediaFormat);
                        PushLog.d("onOutputFormatChanged -----------> sps len=" + sps.length + ",pps len =" + pps.length);
                        ScreenEncoder.ScreenEncoderCallback callback = ScreenEncoderHandler.this.getCallback();
                        if (callback != null) {
                            callback.onVideoSequenceHeader(sps, sps.length, pps, pps.length, false);
                        }

                        if (ScreenEncoderHandler.this.baseMp4Muxer != null) {
                            ScreenEncoderHandler.this.baseMp4Muxer.storeMediaFormat(10, mediaFormat, sps, pps);
                        }

                    }
                });
            }

            try {
                this.hwEncoder.configure(this.videoFormat, (Surface) null, (MediaCrypto) null, 1);
                Surface encoderSurface = this.hwEncoder.createInputSurface();
                this.initMediaCodecGL(encoderSurface);
                this.hwEncoder.start();
                if (this.isAsyncMode) {
                    this.screenDataThread = new ScreenDataThread(this.hwEncoder, this.isAsyncMode, this.getCallback(), this.baseMp4Muxer);
                    this.screenDataThread.start();
                }

                this.drawFrameRateMeter.reSet();
                PushLog.i("initMediaCodec success");
            } catch (Exception var8) {
                var8.printStackTrace();
                this.releaseHWEncoder();
                this.releaseMediaCodecGL();
                if (this.getCallback() != null) {
                    this.getCallback().onError(12, "Fail to start MediaCodec");
                }
            }

        }

        private void deliverEncodedImage(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
            try {
                ByteBuffer codecOutputBuffer = codec.getOutputBuffer(outputBufferId);
                if ((info.flags & 2) != 0) {
                    PushLog.d("Config frame generated. Offset: " + info.offset + ". Size: " + info.size);
                } else {
                    ScreenEncoder.ScreenEncoderCallback callback = this.getCallback();
                    if (callback != null && this.retryCount < 2 && 0L != this.mLastPts && info.presentationTimeUs < this.mLastPts) {
                        callback.onVideoEncodeError(1);
                        PushLog.e("Not support high profile!");
                        this.enableHighMode = 0;
                        this.stopStreamInner();
                        this.startStreamInner(this.mOrientation);
                        ++this.retryCount;
                        return;
                    }

                    this.mLastPts = info.presentationTimeUs;
                    boolean isKeyFrame = (info.flags & 1) != 0;
                    if (isKeyFrame) {
                        PushLog.d("Sync frame generated");
                    }

                    long currTime = System.nanoTime() / 1000L;
                    if (this.encoderStartTime == 0L) {
                        this.encoderStartTime = currTime;
                    }

                    if (info.size > 0) {
                        long tms = currTime - this.encoderStartTime;
                        if (this.baseMp4Muxer != null) {
                            ByteBuffer dataForMp4 = codecOutputBuffer.duplicate();
                            MediaCodec.BufferInfo bufferInfo = MediaCodecHelper.cloneBufferInfo(info);
                            bufferInfo.presentationTimeUs = tms;
                            this.baseMp4Muxer.writeSampleData(10, dataForMp4, bufferInfo);
                        }

                        codecOutputBuffer.position(info.offset + 4);
                        codecOutputBuffer.limit(info.offset + info.size);
                        this.sendRealData(tms / 1000L, codecOutputBuffer, isKeyFrame);
                    }
                }

                this.encodeErrorNum = 0;
                codec.releaseOutputBuffer(outputBufferId, false);
            } catch (Exception var13) {
                var13.printStackTrace();
                ++this.encodeErrorNum;
                if (this.encodeErrorNum > 100 && this.getCallback() != null) {
                    this.getCallback().onVideoEncodeError(0);
                }
            }

        }

        private void sendRealData(long tms, ByteBuffer realData, boolean isKeyframe) {
            int realDataLength = realData.remaining();
            byte[] finalBuff = new byte[realDataLength];
            realData.get(finalBuff, 0, realDataLength);
            int frameType = finalBuff[0] & 31;
            ScreenEncoder.ScreenEncoderCallback callback = this.getCallback();
            if (callback != null) {
                callback.onVideoPacket(finalBuff, realDataLength, (int) tms, isKeyframe);
            }

        }

        private byte[] getSPSByte(MediaFormat mediaFormat) {
            ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
            SPSByteBuff.position(4);
            int spslength = SPSByteBuff.remaining();
            byte[] sps = new byte[spslength];
            SPSByteBuff.get(sps, 0, spslength);
            SPSByteBuff.rewind();
            return sps;
        }

        private byte[] getPPSByte(MediaFormat mediaFormat) {
            ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
            PPSByteBuff.position(4);
            int ppslength = PPSByteBuff.remaining();
            byte[] pps = new byte[ppslength];
            PPSByteBuff.get(pps, 0, ppslength);
            PPSByteBuff.rewind();
            return pps;
        }

        private void releaseHWEncoder() {
            if (this.hwEncoder != null) {
                try {
                    this.hwEncoder.stop();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }

                this.hwEncoder.release();
                this.hwEncoder = null;
                this.videoFormat = null;
            }

        }

        private void initMediaCodecGL(Surface surface) {
            GLES20.glEnable(36197);
            if (this.mMediaCodecGLWrapper == null && this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null) {
                this.mMediaCodecGLWrapper = new CmmScreenGLWrapper();

                try {
                    this.mMediaCodecGLWrapper.mEglBase = EglBase.create(this.mOffScreenGLWrapper.mEglBase.getEglBaseContext(), EglBase.CONFIG_RECORDABLE);
                    this.mMediaCodecGLWrapper.mEglBase.createSurface(surface);
                    this.mMediaCodecGLWrapper.mEglBase.makeCurrent();
                } catch (Exception var3) {
                    var3.printStackTrace();
                    this.releaseMediaCodecGL();
                    if (this.getCallback() != null) {
                        this.getCallback().onError(12, "Fail to create MediaCodec");
                    }

                    return;
                }

                this.initMediaCodecGLProgram();
            }

        }

        private void initMediaCodecGLProgram() {
            if (this.mMediaCodecGLWrapper != null) {
                this.mMediaCodecGLWrapper.mProgram = GlUtil.createProgram(
                        "uniform mat4 uMVPMatrix;                   \n" +
                                "uniform mat4 uSTMatrix;                           \n" +
                                "attribute vec4 aPosition;                         \n" +
                                "attribute vec4 aTextureCoord;                     \n" +
                                "varying vec2 vTextureCoord;                       \n" +
                                "void main() {                                     \n" +
                                "  gl_Position = uMVPMatrix * aPosition;           \n" +
                                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy; \n" +
                                "}                                                 \n"
                        , FRAGMENT_SHADER_SAMPLE2D);
                if (this.mMediaCodecGLWrapper.mProgram == 0) {
                    throw new RuntimeException("failed creating program");
                }

                this.mMediaCodecGLWrapper.maPositionHandle = GLES20.glGetAttribLocation(this.mMediaCodecGLWrapper.mProgram, "aPosition");
                GlUtil.checkNoGLES2Error("glGetAttribLocation aPosition");
                if (this.mMediaCodecGLWrapper.maPositionHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for aPosition");
                }

                this.mMediaCodecGLWrapper.maTextureHandle = GLES20.glGetAttribLocation(this.mMediaCodecGLWrapper.mProgram, "aTextureCoord");
                GlUtil.checkNoGLES2Error("glGetAttribLocation aTextureCoord");
                if (this.mMediaCodecGLWrapper.maTextureHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for aTextureCoord");
                }

                this.mMediaCodecGLWrapper.muMVPMatrixHandle = GLES20.glGetUniformLocation(this.mMediaCodecGLWrapper.mProgram, "uMVPMatrix");
                GlUtil.checkNoGLES2Error("glGetUniformLocation uMVPMatrix");
                if (this.mMediaCodecGLWrapper.muMVPMatrixHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for uMVPMatrix");
                }

                this.mMediaCodecGLWrapper.muSTMatrixHandle = GLES20.glGetUniformLocation(this.mMediaCodecGLWrapper.mProgram, "uSTMatrix");
                GlUtil.checkNoGLES2Error("glGetUniformLocation uSTMatrix");
                if (this.mMediaCodecGLWrapper.muSTMatrixHandle == -1) {
                    throw new RuntimeException("Could not get attrib location for uSTMatrix");
                }

                this.mMediaCodecGLWrapper.musTexture = GLES20.glGetUniformLocation(this.mMediaCodecGLWrapper.mProgram, "sTexture");
                GlUtil.checkNoGLES2Error("glGetUniformLocation sTexture");
                if (this.mMediaCodecGLWrapper.musTexture == -1) {
                    throw new RuntimeException("Could not get attrib location for sTexture");
                }
            }

        }

        private void releaseMediaCodecGL() {
            if (this.mMediaCodecGLWrapper != null && this.mMediaCodecGLWrapper.mEglBase != null) {
                try {
                    this.mMediaCodecGLWrapper.mEglBase.makeCurrent();
                    if (this.mMediaCodecGLWrapper.mProgram != 0) {
                        GLES20.glDeleteProgram(this.mMediaCodecGLWrapper.mProgram);
                    }
                } catch (Exception var3) {
                    var3.printStackTrace();
                }

                try {
                    this.mMediaCodecGLWrapper.mEglBase.release();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }

                this.mMediaCodecGLWrapper.mEglBase = null;
                this.mMediaCodecGLWrapper = null;
            }

        }

        private void calcAdaptMediaCodecMatrix(float mVideoAspectRatio, float mScreenAspectRatio) {
            if (mVideoAspectRatio != 0.0F && mScreenAspectRatio != 0.0F) {
                float xScale = 1.0F;
                float yScale = 1.0F;
                Matrix.setIdentityM(this.mVerticesTransformMatrix, 0);
                if (mScreenAspectRatio > 1.0F) {
                    if (mVideoAspectRatio > 1.0F) {
                        if (mVideoAspectRatio < mScreenAspectRatio) {
                            xScale = mScreenAspectRatio / mVideoAspectRatio;
                        } else {
                            yScale = mVideoAspectRatio / mScreenAspectRatio;
                        }

                        Matrix.scaleM(this.mVerticesTransformMatrix, 0, xScale, yScale, 1.0F);
                    } else {
                        yScale = mVideoAspectRatio / mScreenAspectRatio;
                        float transY = (1.0F - yScale) / 2.2F;
                        Matrix.translateM(this.mVerticesTransformMatrix, 0, 0.0F, transY, 0.0F);
                        Matrix.scaleM(this.mVerticesTransformMatrix, 0, 1.0F, yScale, 1.0F);
                    }
                } else if (mVideoAspectRatio > 1.0F) {
                    xScale = mScreenAspectRatio / mVideoAspectRatio;
                    Matrix.scaleM(this.mVerticesTransformMatrix, 0, xScale, 1.0F, 1.0F);
                } else {
                    if (mVideoAspectRatio < mScreenAspectRatio) {
                        xScale = mScreenAspectRatio / mVideoAspectRatio;
                    } else {
                        yScale = mVideoAspectRatio / mScreenAspectRatio;
                    }

                    Matrix.scaleM(this.mVerticesTransformMatrix, 0, xScale, yScale, 1.0F);
                }

            }
        }

        private void updateOESSurfaceTextureTexImageInner() {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null && this.mSurfaceTexture != null) {
                this.mOffScreenGLWrapper.mEglBase.makeCurrent();
                synchronized (this.mFrameUpdateLock) {
                    if (this.updateSurface) {
                        this.updateSurface = false;

                        try {
                            this.mSurfaceTexture.updateTexImage();
                        } catch (Exception var7) {
                            var7.printStackTrace();
                            return;
                        }

                        if (this.screenCaptureFrameRateMeter != null) {
                            this.screenCaptureFrameRateMeter.count();
                        }

                        long currTime = System.currentTimeMillis();
                        long diffTime = currTime - this.lastFrameAvailableTime;
                        if (diffTime < this.limitMaxFpsTime) {
                            return;
                        }

                        this.lastFrameAvailableTime = currTime;
                        this.hasNewFrame = true;
                    }
                }

                this.drawSample2DFrameBuffer(this.mSurfaceTexture);
            }

        }

        private void drawSample2DFrameBuffer(SurfaceTexture st) {
            if (this.mOffScreenGLWrapper != null && this.mOffScreenGLWrapper.mEglBase != null && st != null && this.hasCreateFrameBuffers && this.oesTextureId != 0) {
                st.getTransformMatrix(this.mSTMatrix);
                GLES20.glBindFramebuffer(36160, this.sample2DFrameBuffer);
                GLES20.glClearColor(0.114F, 0.169F, 0.184F, 1.0F);
                GLES20.glClear(16640);
                GLES20.glUseProgram(this.mOffScreenGLWrapper.mProgram);

                try {
                    GlUtil.checkNoGLES2Error("glUseProgram");
                } catch (Exception var3) {
                    var3.printStackTrace();
                    GLES20.glUseProgram(0);
                    return;
                }

                GLES20.glActiveTexture(33984);
                GLES20.glBindTexture(36197, this.oesTextureId);
                GLES20.glUniform1i(this.mOffScreenGLWrapper.musTexture, 0);
                GLES20.glEnableVertexAttribArray(this.mOffScreenGLWrapper.maPositionHandle);
                GlUtil.checkNoGLES2Error("glEnableVertexAttribArray maPositionHandle");
                GLES20.glVertexAttribPointer(this.mOffScreenGLWrapper.maPositionHandle, 2, 5126, false, 8, this.shapeVerticesBuffer);
                GlUtil.checkNoGLES2Error("glVertexAttribPointer maPosition");
                GLES20.glEnableVertexAttribArray(this.mOffScreenGLWrapper.maTextureHandle);
                GlUtil.checkNoGLES2Error("glEnableVertexAttribArray maTextureHandle");
                GLES20.glVertexAttribPointer(this.mOffScreenGLWrapper.maTextureHandle, 2, 5126, false, 8, this.screenCaptureTextureBuffer);
                GlUtil.checkNoGLES2Error("glVertexAttribPointer maTextureHandle");
                Matrix.setIdentityM(this.mMVPMatrix, 0);
                GLES20.glUniformMatrix4fv(this.mOffScreenGLWrapper.muMVPMatrixHandle, 1, false, this.mMVPMatrix, 0);
                GLES20.glUniformMatrix4fv(this.mOffScreenGLWrapper.muSTMatrixHandle, 1, false, this.mSTMatrix, 0);
                GLES20.glViewport(0, 0, this.captureWidth, this.captureHeight);
                GLES20.glDrawArrays(5, 0, 4);
                GlUtil.checkNoGLES2Error("glDrawArrays");
                GLES20.glFinish();
                GLES20.glDisableVertexAttribArray(this.mOffScreenGLWrapper.maPositionHandle);
                GLES20.glDisableVertexAttribArray(this.mOffScreenGLWrapper.maTextureHandle);
                GLES20.glBindTexture(36197, 0);
                GLES20.glUseProgram(0);
                GLES20.glBindFramebuffer(36160, 0);
            }
        }

        private void drawFrameInner(long time) {
            long interval = time + this.oneFrameDuration - SystemClock.uptimeMillis();
            if (this.isStreaming) {
                this.sendDrawFrameMsg(interval);
            }

            long currTime = System.currentTimeMillis();
            if (this.hasNewFrame && this.lastDrawFrameTime == 0L || this.lastDrawFrameTime > 0L) {
                this.drawFrameToMediaCodec(time * 1000000L);
                this.hasNewFrame = false;
                this.lastDrawFrameTime = currTime;
                if (this.drawFrameRateMeter != null) {
                    this.drawFrameRateMeter.count();
                }
            }

        }

        private void drawFrameToMediaCodec(long time) {
            if (this.mMediaCodecGLWrapper != null && this.mMediaCodecGLWrapper.mEglBase != null && this.hasCreateFrameBuffers && this.isStreaming) {
                try {
                    this.mMediaCodecGLWrapper.mEglBase.makeCurrent();
                    GlUtil.checkNoGLES2Error("onDrawFrame start");
                } catch (Exception var7) {
                    var7.printStackTrace();
                    return;
                }

                GLES20.glClearColor(0.114F, 0.169F, 0.184F, 1.0F);
                GLES20.glClear(16640);
                if (this.isDisableVideo) {
                    try {
                        if (null != this.markTexture) {
                            this.markTexture.draw();
                        }

                        this.mMediaCodecGLWrapper.mEglBase.swapBuffers(time);
                    } catch (Exception var4) {
                        var4.printStackTrace();
                    }

                } else {
                    GLES20.glUseProgram(this.mMediaCodecGLWrapper.mProgram);

                    try {
                        GlUtil.checkNoGLES2Error("glUseProgram");
                    } catch (Exception var6) {
                        var6.printStackTrace();
                        GLES20.glUseProgram(0);
                        return;
                    }

                    GLES20.glActiveTexture(33984);
                    GLES20.glBindTexture(3553, this.sample2DFrameBufferTexture);
                    GLES20.glUniform1i(this.mMediaCodecGLWrapper.musTexture, 0);
                    GLES20.glEnableVertexAttribArray(this.mMediaCodecGLWrapper.maPositionHandle);
                    GlUtil.checkNoGLES2Error("glEnableVertexAttribArray maPositionHandle");
                    GLES20.glVertexAttribPointer(this.mMediaCodecGLWrapper.maPositionHandle, 2, 5126, false, 8, this.shapeVerticesBuffer);
                    GlUtil.checkNoGLES2Error("glVertexAttribPointer maPosition");
                    GLES20.glEnableVertexAttribArray(this.mMediaCodecGLWrapper.maTextureHandle);
                    GlUtil.checkNoGLES2Error("glEnableVertexAttribArray maTextureHandle");
                    GLES20.glVertexAttribPointer(this.mMediaCodecGLWrapper.maTextureHandle, 2, 5126, false, 8, this.mediaCodecTextureBuffer);
                    GlUtil.checkNoGLES2Error("glVertexAttribPointer maTextureHandle");
                    GLES20.glUniformMatrix4fv(this.mMediaCodecGLWrapper.muMVPMatrixHandle, 1, false, this.mVerticesTransformMatrix, 0);
                    Matrix.setIdentityM(this.mSTMatrix, 0);
                    GLES20.glUniformMatrix4fv(this.mMediaCodecGLWrapper.muSTMatrixHandle, 1, false, this.mSTMatrix, 0);
                    GLES20.glViewport(0, 0, this.outputWidth, this.outputHeight);
                    GLES20.glDrawArrays(5, 0, 4);
                    GlUtil.checkNoGLES2Error("glDrawArrays");
                    GLES20.glFinish();
                    GLES20.glDisableVertexAttribArray(this.mMediaCodecGLWrapper.maPositionHandle);
                    GLES20.glDisableVertexAttribArray(this.mMediaCodecGLWrapper.maTextureHandle);
                    GLES20.glBindTexture(3553, 0);
                    GLES20.glUseProgram(0);

                    try {
                        this.mMediaCodecGLWrapper.mEglBase.swapBuffers(time);
                    } catch (Exception var5) {
                        var5.printStackTrace();
                    }

                }
            }
        }

        private void checkGreenScreen() {
            int width = this.outputWidth;
            int height = this.outputHeight;
            if (width >= 10 && height >= 10) {
                long startTime = System.currentTimeMillis();
                ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                GLES20.glReadPixels(0, 0, width, height, 6408, 5121, buf);
                buf.rewind();
                int pixelCount = width * height;
                int[] colors = new int[pixelCount];
                buf.asIntBuffer().get(colors);
                int[] checkPointIndex = new int[]{width * height / 16 - 1, width * 3 / 4 * (height / 4) - 1, width * height / 4 - 1, width / 4 * (height * 3 / 4) - 1, width * 3 / 4 * (height * 3 / 4) - 1};

                for (int i = 0; i < checkPointIndex.length; ++i) {
                    int c = colors[checkPointIndex[i]];
                    colors[checkPointIndex[i]] = c & -16711936 | (c & 16711680) >> 16 | (c & 255) << 16;
                }

                float[] hsv01 = new float[3];
                Color.colorToHSV(colors[checkPointIndex[0]], hsv01);
                float[] hsv02 = new float[3];
                Color.colorToHSV(colors[checkPointIndex[1]], hsv02);
                float[] hsv03 = new float[3];
                Color.colorToHSV(colors[checkPointIndex[2]], hsv03);
                float[] hsv04 = new float[3];
                Color.colorToHSV(colors[checkPointIndex[3]], hsv04);
                float[] hsv05 = new float[3];
                Color.colorToHSV(colors[checkPointIndex[4]], hsv05);
                StringBuilder sb = new StringBuilder();
                sb.append("[[[drawNum=");
                sb.append(",color[").append(checkPointIndex[0]).append("]=").append(Integer.toHexString(colors[checkPointIndex[0]]));
                sb.append(",color[").append(checkPointIndex[1]).append("]=").append(Integer.toHexString(colors[checkPointIndex[1]]));
                sb.append(",color[").append(checkPointIndex[2]).append("]=").append(Integer.toHexString(colors[checkPointIndex[2]]));
                sb.append(",color[").append(checkPointIndex[3]).append("]=").append(Integer.toHexString(colors[checkPointIndex[3]]));
                sb.append(",color[").append(checkPointIndex[4]).append("]=").append(Integer.toHexString(colors[checkPointIndex[4]]));
                sb.append(",hsv1=").append(hsv01[0]).append(",").append(hsv01[1]).append(",").append(hsv01[2]);
                sb.append(",hsv2=").append(hsv02[0]).append(",").append(hsv02[1]).append(",").append(hsv02[2]);
                sb.append(",hsv3=").append(hsv03[0]).append(",").append(hsv03[1]).append(",").append(hsv03[2]);
                sb.append(",hsv4=").append(hsv04[0]).append(",").append(hsv04[1]).append(",").append(hsv04[2]);
                sb.append(",hsv5=").append(hsv05[0]).append(",").append(hsv05[1]).append(",").append(hsv05[2]);
                sb.append("]]] ");
                PushLog.d("checkscreen=" + sb.toString());
            }
        }
    }
}
