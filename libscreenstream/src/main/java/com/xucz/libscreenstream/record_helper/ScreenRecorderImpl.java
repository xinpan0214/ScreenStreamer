package com.xucz.libscreenstream.record_helper;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.projection.MediaProjection;

import com.xucz.libscreenstream.NonoRecordHelper;
import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.entity.VideoSize;
import com.xucz.libscreenstream.helper.MediaCodecHelper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.pusher.screenstream.ScreenStream;
import com.xucz.libscreenstream.recorder.RecordHandler;
import com.xucz.libscreenstream.recorder.ScreenRecordManager;
import com.xucz.libscreenstream.utils.FileUtils;

/**
 * Description:向未来录制30秒流的辅助类：适用于秀场开播，直播间向后录制30秒
 * sdk中实现了统一的录屏，推流，合成mp4的逻辑
 *
 * @author 杜乾, Created on 2018/4/24 - 20:41.
 * E-mail:duqian2010@gmail.com
 */
public class ScreenRecorderImpl implements IScreenRecorder {
    private static final String TAG = ScreenRecorderImpl.class.getSimpleName() + " dq-";
    private ScreenRecordManager screenRecordManager;
    private ScreenStream screenStream;
    private PrepareInfo mPrepareInfo = null;
    private boolean isPushRoom = false;//是否为开播间

    public ScreenRecorderImpl(boolean isPushRoom, RecordCallBack recordCallBack) {
        this.isPushRoom = isPushRoom;
        this.callback = recordCallBack;
    }

    @Override
    public void initRecorder(PrepareInfo prepareInfo) {
        this.mPrepareInfo = prepareInfo;
    }

    @Override
    public void stopRecord() {
        if (screenRecordManager != null) {
            screenRecordManager.stopRecord();
            RecordMuxerHelper.get().remove(screenRecordManager.getBaseMp4Muxer());
            screenRecordManager = null;
        }
        if (screenStream != null) {
            screenStream.stop();
            screenStream = null;
        }
        removeCallback();//移除callback
    }

    private void removeCallback() {
        if (callback != null) {
            callback = null;
        }
    }

    @TargetApi(21)
    @Override
    public void recordScreen(Intent data, int mOrientation) {
        if (data == null) {
            return;
        }
        screenStream = new ScreenStream(!isPushRoom);
        initScreenRecordManager();
        Configure config = initScreenRecordConfigure();
        final boolean prepare = screenStream.prepare(config, screenRecordManager, null, data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                //Logger.d("dq MediaProjection onStop=====");
            }
        }, mPrepareInfo.context, mPrepareInfo.screenDpi);
        screenStream.start(mOrientation);
        //Logger.d("dq recordScreen =====" + mOrientation + ",prepare=" + prepare);
    }

    private void startRecord() {
        if (screenRecordManager != null) {
            final boolean startRecord = screenRecordManager.startRecord(mPrepareInfo.savePath);//mOrientation
            PushLog.d(TAG, "live startRecord=" + startRecord);
            NonoRecordHelper.get().setRecordingLive(startRecord);
        }
    }

    private void initScreenRecordManager() {
        if (isPushRoom) {
            screenRecordManager = ScreenRecordManager.getPushRoomRecorder(false);
        } else {
            screenRecordManager = ScreenRecordManager.getLiveRoomRecorder();
        }
        screenRecordManager.setRecordHandler(new RecordHandler(new RecordHandler.RecordListener() {
            @Override
            public void onRecordPrepared() {
                //Logger.d(TAG, "dq screenRecorder1 onRecordPrepared");
                startRecord();
            }

            @Override
            public void onRecordStarted(String filePath) {
                //Logger.d(TAG, "dq screenRecorder2 start=" + filePath);
                if (callback != null) {
                    callback.onStart();
                }
            }

            @Override
            public void onRecordStopped(Exception e, String filePath) {
                PushLog.d(TAG, "dq2 screenRecorder3 onRecordStopped =" + e + "filePath=" + filePath);
                if (e != null) {
                    FileUtils.deleteFile(mPrepareInfo.savePath);//删除录制的文件
                }
                if (callback != null) {
                    callback.onStop(e);
                }
            }
        }));

        RecordMuxerHelper.get().add(screenRecordManager.getBaseMp4Muxer());
    }

    private Configure initScreenRecordConfigure() {
        Configure configure = new Configure();
        // 初始化参数
        final boolean isLandscape = mPrepareInfo.isLandscape;
        int height = mPrepareInfo.screenHeight;
        int width = mPrepareInfo.screenWidth;
        //按比例调整视频尺寸，降低size，防止其他分辨率出现黑边
        float ratio = height * 1.0f / width;
        if (ratio == 1.0f * 16 / 9 && width >= 720) {//4:3分辨率
            width = 540;
            height = 960;
        } else if (width >= 1080) {//18:9的屏幕比例
            width = width / 2;
            height = height / 2;
        }
        final int videoWidth = isLandscape ? height : width;
        final int videoHeight = isLandscape ? width : height;
        PushLog.d(TAG, isLandscape + ",videoWidth=" + videoWidth + ",videoHeight=" + videoHeight);

        configure.previewVideoWidth = videoWidth;
        configure.previewVideoHeight = videoHeight;

        configure.videoWidth = videoWidth;
        configure.videoHeight = videoHeight;

        configure.videoFPS = 25;
        configure.screenCaptureMaxFps = 25;
        configure.videoBitRate = 1024 * 1024;

        if (!MediaCodecHelper.isSizeSupported(configure.videoWidth, configure.videoHeight)) {
            configure.videoWidth = 540;
            configure.videoHeight = 960;
            //Logger.d("dq isSizeSupported false 540 960");
        }

        configure.setFilterMode(Configure.FilterMode.FILTER_MODE_HARD);
        configure.setTargetVideoSize(new VideoSize(configure.previewVideoWidth, configure.previewVideoHeight));

        configure.soft_enable_server = 1;
        configure.hasDetectSoftEncode = true;
        configure.enableSoftEncode = false;//是否使用软编码

        //代码加强处理,防止服务器返回的数据是0的情况
        if (configure.previewVideoHeight == 0 ||
                configure.previewVideoWidth == 0 ||
                configure.videoFPS == 0) {
            configure.previewVideoHeight = Configure.VideoConfig.PREVIEW_HEIGHT;
            configure.previewVideoWidth = Configure.VideoConfig.PREVIEW_WIDTH;
            configure.setTargetVideoSize(new VideoSize(Configure.VideoConfig.PREVIEW_WIDTH, Configure.VideoConfig.PREVIEW_HEIGHT));
            configure.videoFPS = Configure.VideoConfig.FRAME_RATE;
            configure.overflowBufferTime = Configure.VideoConfig.OVERFLOW_BURRER_TIME;
            configure.trashingBufferTime = Configure.VideoConfig.TRASHING_BUFFER_TIME;
            configure.trashingCountThreshold = Configure.VideoConfig.TRASHING_COUNT_THRESHOLD;
        }
        configure.setRenderingMode(Configure.RenderingMode.RENDERING_MODE_OPENGLES);
        return configure;
    }

    private RecordCallBack callback;

    @Override
    public void setCallback(RecordCallBack callback) {
        this.callback = callback;
    }

}
