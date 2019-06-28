package com.xucz.libscreenstream.record_helper;

import android.content.Context;
import android.text.TextUtils;

import com.xucz.libscreenstream.NonoRecordHelper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.recorder.RecordHandler;
import com.xucz.libscreenstream.recorder.ScreenRecordManager;
import com.xucz.libscreenstream.utils.FileUtils;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.xucz.libscreenstream.NonoRecordHelper.MAX_VIDEO_NUM;
import static com.xucz.libscreenstream.NonoRecordHelper.RECORD_INTERVAL_TIME_SECOND;
import static com.xucz.libscreenstream.NonoRecordHelper.RECORD_VIDEO_SUFFIX;

/**
 * Description: 开播间的视频流录制，手游开播一键录制,
 * <p>
 * 该管理器注入到推流sdk初始化配置中，实现了混合视频和音频为mp4的逻辑
 * <p>
 * 回调方法可以拿到录制状态：准备好了视频头，sps，pps数据，开始录制，结束录制某个文件
 *
 * @author 杜乾, Created on 2018/5/11 - 14:53.
 * E-mail:duqian2010@gmail.com
 */
public class LiveRecorderImpl implements ILiveRecorder {

    private static final String TAG = LiveRecorderImpl.class.getSimpleName() + " dq-";
    private ScreenRecordManager screenRecordManager;
    private Queue<String> linkedList = new ConcurrentLinkedQueue<>();
    private boolean isPushRoom = true;//是否是秀场开播录制，false为手游开播
    private Context context;
    private String rootTempDir;

    public LiveRecorderImpl(boolean isPushRoom, Context context) {
        this.isPushRoom = isPushRoom;
        this.context = context;
        initRecorder();
    }

    public ScreenRecordManager getScreenRecordManager() {
        return screenRecordManager;
    }

    @Override
    public void initRecorder() {
        linkedList = RecordStoreManager.getInstance().mVideoLinkedList;
        lastClearCacheTime = 0;
        rootTempDir = RecordStoreManager.getInstance().getLiveVideoTempDir() + File.separator;
        FileUtils.deleteFile(rootTempDir);

        clearRecordCache();
        initScreenRecordManager();
    }

    private long lastClearCacheTime = 0;//至少1秒清理一次，防止一次旋转屏幕多次回调，多次清理？

    public void clearRecordCache() {
        if (System.currentTimeMillis() - lastClearCacheTime <= 1000) {
            return;
        }
        if (linkedList != null) {
            linkedList.clear();//先清空
        }
        if (!isPushRoom && context != null) {
            NonoRecordHelper.resetGameLiveStartTime(context, System.currentTimeMillis());
        }
        //Logger.d(TAG, "clearRecordCache ");
        lastClearCacheTime = System.currentTimeMillis();
    }

    private void initScreenRecordManager() {
        if (isPushRoom) {
            screenRecordManager = ScreenRecordManager.getPushRoomRecorder(true);
        } else {
            screenRecordManager = ScreenRecordManager.getMobileGameLiveRecorder();
        }
        screenRecordManager.setRecordIntervalTime(RECORD_INTERVAL_TIME_SECOND);//间隔两秒存一个文件
        screenRecordManager.setRecordVideoSuffix(RECORD_VIDEO_SUFFIX);//缓存文件后缀名

        screenRecordManager.setRecordHandler(new RecordHandler(new RecordHandler.RecordListener() {
            @Override
            public void onRecordPrepared() {//只在相机预览后开始编码后，回调一次
                //Logger.d(TAG, "live onRecordPrepared");
            }

            @Override
            public void onRecordStarted(String filePath) {//会有多次回调
                //Logger.d(TAG, "dudu live onRecordStarted=" + filePath);
            }

            @Override
            public void onRecordStopped(Exception e, String filePath) {//会有多次回调
                if (e != null) {
                    FileUtils.deleteFile(filePath);
                    PushLog.d(TAG, "live onRecordStopped=" + e + ",filePath=" + filePath);
                } else {
                    handleStopped(filePath);
                }
            }
        }));
    }

    /**
     * 将回调回来的录制好的文件，缓存到队列，维护xx个文件
     *
     * @param filePath
     */
    private void handleStopped(String filePath) {
        if (linkedList == null || TextUtils.isEmpty(filePath) || linkedList.contains(filePath)) {
            return;
        }
        try {
            linkedList.add(filePath);
            if (linkedList.size() > MAX_VIDEO_NUM + 2) {//具体要计算;在实际基础上多两个文件
                final String poll = linkedList.poll();
                FileUtils.deleteFile(poll);
            }
            //Logger.d(TAG, "live mVideoLinkedList,size=" + linkedList.size() + ",add=" + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void startRecord() {//开始录制，或者重启录制都调用这个方法
        if (screenRecordManager != null) {
            clearRecordCache();//清掉缓存
            final boolean startRecord = screenRecordManager.startRecord(rootTempDir);
            //Logger.d(TAG, "start live Record=" + startRecord);
            //记录是否开始录制mp4，开播间需要判断是否可以点击向前录制
            NonoRecordHelper.get().setRtmpPusherRecording(startRecord);
            NonoRecordHelper.resetStartRecordPushTime(context, System.currentTimeMillis());
        }
    }

    @Override
    public void stopRecord() {
        if (screenRecordManager != null) {
            screenRecordManager.stopRecord();
            screenRecordManager = null;
            RecordStoreManager.getInstance().clearLiveTempFiles();//清除本地录制缓存文件
            NonoRecordHelper.get().setRtmpPusherRecording(false);
        }
    }

    private RecordCallBack callback;

    @Override
    public void setCallback(RecordCallBack callback) {
        this.callback = callback;
    }

}
