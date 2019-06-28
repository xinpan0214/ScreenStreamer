package com.xucz.libscreenstream.pusher.screenstream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.gles.utils.ThreadUtils;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;
import com.xucz.libscreenstream.rtmp.IFlvDataCollect;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */

@TargetApi(21)
public class ScreenCapturer {
    private static final int DISPLAY_FLAGS = 3;
    private static final int VIRTUAL_DISPLAY_DPI = 400;
    private int mScreenDensity = 400;
    private final Intent mediaProjectionPermissionResultData;
    private final MediaProjection.Callback mediaProjectionCallback;
    private int width;
    private int height;
    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private ScreenEncoder mScreenEncoder;
    private SurfaceTexture mSurfaceTexture = null;
    private final Object mLock = new Object();
    private int mOrientation = 0;
    private volatile boolean isPendingStartStream = false;
    private Handler mHandler;
    private IFlvDataCollect flvDataCollecter;
    private ScreenStream.IEncoderListener encoderListener;
    private BaseMp4Muxer baseMp4Muxer;

    public ScreenCapturer(Intent mediaProjectionPermissionResultData, Configure configure, BaseMp4Muxer baseMp4Muxer, MediaProjection.Callback mediaProjectionCallback) {
        this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData;
        this.mediaProjectionCallback = mediaProjectionCallback;
        this.width = configure.previewVideoWidth;
        this.height = configure.previewVideoHeight;
        this.baseMp4Muxer = baseMp4Muxer;
        HandlerThread thread = new HandlerThread("scThread");
        thread.start();
        this.mHandler = new Handler(thread.getLooper());
        this.mScreenEncoder = new ScreenEncoder(configure, baseMp4Muxer, new ScreenEncoder.ScreenEncoderCallback() {
            public void onSurfaceTexture(SurfaceTexture surfaceTexture) {
                synchronized (ScreenCapturer.this.mLock) {
                    ScreenCapturer.this.mSurfaceTexture = surfaceTexture;
                    if (ScreenCapturer.this.isPendingStartStream) {
                        PushLog.i("onSurfaceTexture start===");
                        ScreenCapturer.this.isPendingStartStream = false;
                        ScreenCapturer.this.mScreenEncoder.startStream(ScreenCapturer.this.mOrientation);
                        ScreenCapturer.this.createVirtualDisplay(surfaceTexture);
                    }

                }
            }

            public void onError(int code, String msg) {
                PushLog.e("onError code=" + code + ",msg=" + msg);
                if (ScreenCapturer.this.encoderListener != null) {
                    ScreenCapturer.this.encoderListener.onError(code, msg);
                }

            }

            public void onVideoSequenceHeader(byte[] spsData, int spsSize, byte[] ppsData, int ppsSize, boolean softEncoder) {
                if (ScreenCapturer.this.flvDataCollecter != null) {
                    ScreenCapturer.this.flvDataCollecter.onVideoSequenceHeader(spsData, spsSize, ppsData, ppsSize, softEncoder);
                }

            }

            public void onVideoPacket(byte[] videoData, int videoSize, int timeoffset, boolean isKeyframe) {
                if (ScreenCapturer.this.flvDataCollecter != null) {
                    ScreenCapturer.this.flvDataCollecter.onVideoPacket(videoData, videoSize, timeoffset, isKeyframe);
                }

            }

            public void onVideoEncodeError(int error) {
                if (ScreenCapturer.this.encoderListener != null) {
                    if (1 == error) {
                        ScreenCapturer.this.encoderListener.onError(error, "Not support high profile!");
                    } else {
                        ScreenCapturer.this.encoderListener.onError(error, "onVideoEncodeError");
                    }
                }

            }
        });
        this.mScreenEncoder.init();
    }

    public synchronized void initialize(Context applicationContext, int screenDensity) {
        this.mediaProjectionManager = (MediaProjectionManager) applicationContext.getSystemService("media_projection");
        this.mScreenDensity = screenDensity;
    }

    public void startCapture(int orientation, IFlvDataCollect flvDataCollecter) {
        synchronized (this.mLock) {
            this.mOrientation = orientation;
            this.flvDataCollecter = flvDataCollecter;
            this.mediaProjection = this.mediaProjectionManager.getMediaProjection(-1, this.mediaProjectionPermissionResultData);
            this.mediaProjection.registerCallback(this.mediaProjectionCallback, this.mHandler);
            this.isPendingStartStream = true;
            SurfaceTexture surfaceTexture = this.mSurfaceTexture;
            if (surfaceTexture != null) {
                this.isPendingStartStream = false;
                PushLog.i("startCapture  start===");
                this.mScreenEncoder.startStream(this.mOrientation);
                this.createVirtualDisplay(surfaceTexture);
            }

        }
    }

    public void stopCapture() {
        PushLog.i("stopCapture");
        ThreadUtils.invokeAtFrontUninterruptibly(this.mHandler, new Runnable() {
            public void run() {
                synchronized (ScreenCapturer.this.mLock) {
                    ScreenCapturer.this.isPendingStartStream = false;
                    if (ScreenCapturer.this.virtualDisplay != null) {
                        ScreenCapturer.this.virtualDisplay.release();
                        ScreenCapturer.this.virtualDisplay = null;
                    }

                    if (ScreenCapturer.this.mediaProjection != null) {
                        ScreenCapturer.this.mediaProjection.unregisterCallback(ScreenCapturer.this.mediaProjectionCallback);
                        ScreenCapturer.this.mediaProjection.stop();
                        ScreenCapturer.this.mediaProjection = null;
                    }

                    ScreenCapturer.this.mScreenEncoder.stopStream();
                }
            }
        });
    }

    public void release() {
        PushLog.i("release1");
        ThreadUtils.invokeAtFrontUninterruptibly(this.mHandler, new Runnable() {
            public void run() {
                synchronized (ScreenCapturer.this.mLock) {
                    ScreenCapturer.this.isPendingStartStream = false;
                    ScreenCapturer.this.mSurfaceTexture = null;
                    if (ScreenCapturer.this.mScreenEncoder != null) {
                        PushLog.i("release2");
                        ScreenCapturer.this.mScreenEncoder.release();
                    }

                }
            }
        });
    }

    public void changeBitrate(final int newBitrate) {
        ThreadUtils.invokeAtFrontUninterruptibly(this.mHandler, new Runnable() {
            public void run() {
                synchronized (ScreenCapturer.this.mLock) {
                    if (ScreenCapturer.this.mScreenEncoder != null) {
                        ScreenCapturer.this.mScreenEncoder.changeBitrate(newBitrate);
                    }

                }
            }
        });
    }

    public void toggleVideo(final boolean isDisable) {
        ThreadUtils.invokeAtFrontUninterruptibly(this.mHandler, new Runnable() {
            public void run() {
                synchronized (ScreenCapturer.this.mLock) {
                    if (ScreenCapturer.this.mScreenEncoder != null) {
                        ScreenCapturer.this.mScreenEncoder.toggleVideo(isDisable);
                    }

                }
            }
        });
    }

    public boolean isDisableVideo() {
        synchronized (this.mLock) {
            return this.mScreenEncoder != null ? this.mScreenEncoder.isDisableVideo() : false;
        }
    }

    public void setMarkTexture(Bitmap bitmap) {
        this.mScreenEncoder.setMarkTexture(bitmap);
    }

    public float getVideoCaptureFrameRate() {
        synchronized (this.mLock) {
            return this.mScreenEncoder != null ? this.mScreenEncoder.getVideoCaptureFrameRate() : 0.0F;
        }
    }

    public float getScreenCaptureFrameRate() {
        synchronized (this.mLock) {
            return this.mScreenEncoder != null ? this.mScreenEncoder.getScreenCaptureFrameRate() : 0.0F;
        }
    }

    public void changeCaptureFormat(int orientation) {
        PushLog.d("dq changeCaptureFormat rotation=" + orientation);
        this.mOrientation = orientation;
        boolean isNeedSwap = false;
        if (orientation != 90 && orientation != 270) {
            if (this.width > this.height) {
                isNeedSwap = true;
            }
        } else if (this.width < this.height) {
            isNeedSwap = true;
        }

        if (isNeedSwap) {
            int temp = this.width;
            this.width = this.height;
            this.height = temp;
        }

        if (this.virtualDisplay != null && this.mScreenEncoder != null) {
            ThreadUtils.invokeAtFrontUninterruptibly(this.mHandler, new Runnable() {
                public void run() {
                    synchronized (ScreenCapturer.this.mLock) {
                        SurfaceTexture surfaceTexture = ScreenCapturer.this.mSurfaceTexture;
                        if (ScreenCapturer.this.mScreenEncoder != null && surfaceTexture != null) {
                            if (ScreenCapturer.this.virtualDisplay != null) {
                                ScreenCapturer.this.virtualDisplay.release();
                            }

                            ScreenCapturer.this.mScreenEncoder.updateScreenOrientation(ScreenCapturer.this.mOrientation);
                            ScreenCapturer.this.createVirtualDisplay(surfaceTexture);
                        }

                    }
                }
            });
        }
    }

    private void createVirtualDisplay(SurfaceTexture surfaceTexture) {
        try {
            surfaceTexture.setDefaultBufferSize(this.width, this.height);
            this.virtualDisplay = this.mediaProjection.createVirtualDisplay("NonoScreenCapture", this.width, this.height, this.mScreenDensity, 3, new Surface(surfaceTexture), (android.hardware.display.VirtualDisplay.Callback) null, (Handler) null);
        } catch (Exception var3) {
            var3.printStackTrace();
            if (this.encoderListener != null) {
                this.encoderListener.onError(13, var3.getMessage());
            }
        }

    }

    public void setEncoderListener(ScreenStream.IEncoderListener listener) {
        this.encoderListener = listener;
    }
}

