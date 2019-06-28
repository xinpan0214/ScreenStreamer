package com.xucz.libscreenstream.pusher.screenstream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;

import com.xucz.libscreenstream.callback.IConnectionListener;
import com.xucz.libscreenstream.callback.IPrepareListener;
import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.entity.VideoSize;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.pusher.EventDispatcher;
import com.xucz.libscreenstream.pusher.controller.AudioPushController;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;
import com.xucz.libscreenstream.recorder.ScreenRecordManager;
import com.xucz.libscreenstream.rtmp.IFlvDataCollect;
import com.xucz.libscreenstream.rtmp.ZeusRtmpSenderPlus;
import com.xucz.libscreenstream.utils.FrameRateMeter;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
@TargetApi(21)
public class ScreenStream {
    private AudioPushController audioPushController;
    private ScreenCapturer screenCapturer;
    private final Object SyncOp;
    private Configure configure;
    private ZeusRtmpSenderPlus rtmpSenderPlus;
    private IFlvDataCollect dataCollecter;
    private int currBitRate;
    private boolean isEnterBackground;
    private boolean isResumeBackground;
    private boolean isPreviewMode;
    private ScreenStream.IEncoderListener encoderListener;
    private boolean needAudioController;
    private FrameRateMeter videoEncoderFrameRate;

    public ScreenStream() {
        this(true);
    }

    public ScreenStream(boolean needAudioController) {
        this.currBitRate = 0;
        this.isEnterBackground = false;
        this.isResumeBackground = false;
        this.isPreviewMode = false;
        this.needAudioController = needAudioController;
        this.SyncOp = new Object();
        this.videoEncoderFrameRate = new FrameRateMeter();
        this.videoEncoderFrameRate.reSet();
        EventDispatcher.getInstance();
    }

    public Configure getConfigure() {
        return this.configure;
    }

    public boolean prepare(Configure configure, IPrepareListener prepareListener, Intent mediaProjectionPermissionResultData, MediaProjection.Callback mediaProjectionCallback, Context appConext, int screenDensity) {
        return this.prepare(configure, (ScreenRecordManager) null, prepareListener, mediaProjectionPermissionResultData, mediaProjectionCallback, appConext, screenDensity);
    }

    public boolean prepare(Configure configure, ScreenRecordManager screenRecordManager, IPrepareListener prepareListener, Intent mediaProjectionPermissionResultData, MediaProjection.Callback mediaProjectionCallback, Context appConext, int screenDensity) {
        if (configure == null) {
            return false;
        } else {
            this.configure = configure;
            synchronized (this.SyncOp) {
                this.currBitRate = configure.videoBitRate;
                BaseMp4Muxer baseMp4Muxer = null;
                boolean isEnablePushScreen = true;
                boolean isEnableRecordLiveStream = false;
                if (screenRecordManager != null) {
                    baseMp4Muxer = screenRecordManager.getBaseMp4Muxer();
                    isEnablePushScreen = screenRecordManager.isEnablePushScreen();
                    isEnableRecordLiveStream = screenRecordManager.isEnableRecordLiveStream();
                }

                this.screenCapturer = new ScreenCapturer(mediaProjectionPermissionResultData, configure, baseMp4Muxer, mediaProjectionCallback);
                this.screenCapturer.initialize(appConext, screenDensity);
                if (this.needAudioController) {
                    if (!isEnableRecordLiveStream) {
                        this.audioPushController = new AudioPushController(baseMp4Muxer);
                    } else {
                        this.audioPushController = new AudioPushController();
                    }
                }

                if (null != this.audioPushController) {
                    if (!this.audioPushController.prepare(prepareListener)) {
                        PushLog.e("ZeusPusher audioClient.prepare() failed");
                        return false;
                    }

                    this.audioPushController.setAudioCallback(new AudioPushController.AudioCallback() {
                        public void onAudioRecordError(int error) {
                            if (ScreenStream.this.encoderListener != null) {
                                ScreenStream.this.encoderListener.onError(error, "Audio error");
                            }

                        }
                    });
                }

                if (isEnablePushScreen) {
                    this.initRtmpSender(configure);
                }

                this.initDataCollecter();
                return true;
            }
        }
    }

    private void initRtmpSender(Configure configure) {
        if (!this.isPreviewMode) {
            this.rtmpSenderPlus = new ZeusRtmpSenderPlus();
            this.rtmpSenderPlus.prepare(configure);
        }

    }

    private void initDataCollecter() {
        this.dataCollecter = new IFlvDataCollect() {
            public void onVideoSequenceHeader(byte[] spsData, int spsSize, byte[] ppsData, int ppsSize, boolean softEncoder) {
                PushLog.e("onVideoPacket ppsSize:" + spsSize + ",ppsSize=" + ppsSize);
                if (ScreenStream.this.rtmpSenderPlus != null) {
                    ScreenStream.this.rtmpSenderPlus.sendVideoSequenceHeader(spsData, spsSize, ppsData, ppsSize, softEncoder);
                }

            }

            public void onVideoPacket(byte[] videoData, int videoSize, int timeoffset, boolean isKeyframe) {
                if (ScreenStream.this.rtmpSenderPlus != null && !ScreenStream.this.isEnterBackground) {
                    if (ScreenStream.this.isResumeBackground) {
                        if (!isKeyframe) {
                            return;
                        }

                        ScreenStream.this.isResumeBackground = false;
                    }

                    if (ScreenStream.this.videoEncoderFrameRate != null) {
                        ScreenStream.this.videoEncoderFrameRate.count();
                    }

                    ScreenStream.this.rtmpSenderPlus.sendVideoPacket(videoData, videoSize, timeoffset, isKeyframe);
                }

            }

            public void onAudioSequenceHeader() {
                if (ScreenStream.this.rtmpSenderPlus != null) {
                    ScreenStream.this.rtmpSenderPlus.sendAudioSequenceHeader();
                }

            }

            public void onAudioPacket(byte[] audioData, int audioSize, int timeoffset, boolean hasHeader) {
                if (ScreenStream.this.rtmpSenderPlus != null && !ScreenStream.this.isEnterBackground) {
                    ScreenStream.this.rtmpSenderPlus.sendAudioPacket(audioData, audioSize, timeoffset, hasHeader);
                }

            }

            public void onSoftEncodeDetectFinished(boolean canUseSoftEncode) {
                PushLog.e("onSoftEncodeDetectFinished canUseSoftEncode=" + canUseSoftEncode);
            }

            public void onChangeToHardEncoder() {
            }

            public void onFirstDrawScreen() {
            }

            public void onVideoEncodeError(int error) {
            }
        };
    }

    public void start(int orientation) {
        synchronized (this.SyncOp) {
            if (this.rtmpSenderPlus != null) {
                this.rtmpSenderPlus.start(this.configure.rtmpAddr);
            }

            if (this.screenCapturer != null) {
                this.screenCapturer.startCapture(orientation, this.dataCollecter);
            }

            if (this.audioPushController != null) {
                this.audioPushController.start(this.dataCollecter);
            }

            PushLog.d("RESClient,start()");
        }
    }

    public void stop() {
        synchronized (this.SyncOp) {
            PushLog.e("videoPushController stop---> start");
            if (this.screenCapturer != null) {
                this.screenCapturer.stopCapture();
            }

            PushLog.e("audioPushController stop---> start");
            if (this.audioPushController != null) {
                this.audioPushController.stop();
            }

            PushLog.e("rtmpSender stop---> start");
            if (this.rtmpSenderPlus != null) {
                this.rtmpSenderPlus.stop(true);
            }

            PushLog.d("RESClient,stop()");
        }
    }

    public void release() {
        synchronized (this.SyncOp) {
            PushLog.e("rtmpSender release---> start");
            if (this.rtmpSenderPlus != null) {
                this.rtmpSenderPlus.destroy();
                this.rtmpSenderPlus = null;
            }

            PushLog.e("videoPushController release---> start");
            if (this.screenCapturer != null) {
                this.screenCapturer.release();
                this.screenCapturer = null;
            }

            PushLog.e("audioPushController release---> start");
            if (this.audioPushController != null) {
                this.audioPushController.release();
                this.audioPushController = null;
            }

            this.isPreviewMode = false;
            PushLog.d("RESClient,destroy()");
        }
    }

    public void disConnectRtmp() {
        synchronized (this.SyncOp) {
            if (this.rtmpSenderPlus != null) {
                this.rtmpSenderPlus.disConnectRtmp();
            }

        }
    }

    public void reconnectRtmp() {
        synchronized (this.SyncOp) {
            if (this.rtmpSenderPlus != null) {
                this.rtmpSenderPlus.reconnectRtmp();
            }

        }
    }

    public void changeBitRate(int newBitRate) {
        synchronized (this.SyncOp) {
            if (this.screenCapturer != null) {
                this.screenCapturer.changeBitrate(newBitRate);
            }

        }
    }

    public void changeCaptureFormat(int orientation) {
        synchronized (this.SyncOp) {
            if (this.screenCapturer != null) {
                if (this.rtmpSenderPlus != null) {
                    this.rtmpSenderPlus.clearSpsPps();
                }

                this.screenCapturer.changeCaptureFormat(orientation);
            }

        }
    }

    public void setMarkTexture(Bitmap bitmap) {
        synchronized (this.SyncOp) {
            if (this.screenCapturer != null) {
                this.screenCapturer.setMarkTexture(bitmap);
            }

        }
    }

    public void toggleAudio(boolean isDisable) {
        synchronized (this.SyncOp) {
            if (this.audioPushController != null) {
                this.audioPushController.toggleAudio(isDisable);
            }

        }
    }

    public void toggleVideo(boolean isDisable) {
        synchronized (this.SyncOp) {
            if (this.screenCapturer != null) {
                this.screenCapturer.toggleVideo(isDisable);
            }

        }
    }

    public boolean isDisableAudio() {
        synchronized (this.SyncOp) {
            return this.audioPushController != null ? this.audioPushController.isDisableAudio() : false;
        }
    }

    public boolean isDisableVideo() {
        synchronized (this.SyncOp) {
            return this.screenCapturer != null ? this.screenCapturer.isDisableVideo() : false;
        }
    }

    public void toggleBackgroundMode(boolean enterBackground) {
        synchronized (this.SyncOp) {
            this.isEnterBackground = enterBackground;
            this.isResumeBackground = !enterBackground;
        }
    }

    public void reSendRtmpFlvHeader(boolean softMode) {
        if (this.rtmpSenderPlus != null) {
            this.rtmpSenderPlus.reSendRtmpFlvHeader(softMode);
        }

    }

    public VideoSize getVideoSize() {
        return new VideoSize(this.configure.videoWidth, this.configure.videoHeight);
    }

    public int getVideoCaptureFrameRate() {
        return this.screenCapturer != null ? (int) this.screenCapturer.getVideoCaptureFrameRate() : 0;
    }

    public int getScreenCaptureFrameRate() {
        return this.screenCapturer != null ? (int) this.screenCapturer.getScreenCaptureFrameRate() : 0;
    }

    public int getVideoEncoderFrameRate() {
        return this.videoEncoderFrameRate != null ? (int) this.videoEncoderFrameRate.getFps() : 0;
    }

    public int getVideoSendFrameRate() {
        return this.rtmpSenderPlus != null ? (int) this.rtmpSenderPlus.getVideoTransFps() : 0;
    }

    public float getStreamTransSpeed() {
        return this.rtmpSenderPlus != null ? (float) this.rtmpSenderPlus.getTotalSpeed() : 0.0F;
    }

    public int getCurrBitrate() {
        return this.configure != null ? this.configure.videoBitRate : 0;
    }

    public ZeusRtmpSenderPlus.StatisticsBandwidth getBandwidthStatisticsAndRestart() {
        return this.rtmpSenderPlus != null ? this.rtmpSenderPlus.getBandwidthStatisticsAndRestart() : null;
    }

    public boolean isRtmpConnected() {
        return this.rtmpSenderPlus != null ? this.rtmpSenderPlus.isRtmpConnected() : false;
    }

    public void setConnectionListener(IConnectionListener connectionListener) {
        if (this.rtmpSenderPlus != null) {
            this.rtmpSenderPlus.setConnectionListener(connectionListener);
        }

    }

    public void setEncoderListener(ScreenStream.IEncoderListener listener) {
        this.encoderListener = listener;
        if (this.screenCapturer != null) {
            this.screenCapturer.setEncoderListener(listener);
        }

    }

    public interface IEncoderListener {
        void onError(int var1, String var2);
    }
}
