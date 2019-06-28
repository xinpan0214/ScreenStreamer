package com.xucz.libscreenstream;

import android.content.Context;

import com.xucz.libscreenstream.utils.SharedPreferencesUtil;

/**
 * Description:管理录制状态,常量
 *
 * @author 杜乾, Created on 2018/5/8 - 18:46.
 * E-mail:duqian2010@gmail.com
 */
public class NonoRecordHelper {
    //录制时间常量
    public static final int RECORD_TIME_LINIT_MIN_SECOND = 5;//最少录制5秒
    public static final int RECORD_TIME_LIMIT_MAX_SECOND = 30;//最多录制30秒
    public static final int RECORD_TIME_LESS_LIVE_TIME_SECOND = 35;//至少开播xx秒
    public static final int RECORD_TIME_MAX_MILLISECOND = RECORD_TIME_LIMIT_MAX_SECOND * 1000;//毫秒
    public static final int RECORD_INTERVAL_TIME_SECOND = 2;//录制间隔时间
    public static final int MAX_VIDEO_NUM = RECORD_TIME_LIMIT_MAX_SECOND / RECORD_INTERVAL_TIME_SECOND;//最多缓存多少个文件
    public static final String RECORD_VIDEO_SUFFIX = ".nn";//保存的临时文件拓展名
    private static final String SP_VIDEO_RENDERING_START_TIME = "VIDEO_RENDERING_START_TIME";
    private static final String SP_START_RECORD_PUSH_TIME = "SP_START_RECORD_PUSH_TIME";
    private static final String SP_ORIENTATION_CHANGED_TIME = "ORIENTATION_CHANGED_TIME";
    private volatile boolean isRecordingLive = false;//是否在录制，决定哪些UI要显示与隐藏
    private volatile boolean isRtmpPusherRecording = false;//开播间是否在录制，决定能否点击向前录制按钮

    public static NonoRecordHelper get() {
        return NonoRecordHelper.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final NonoRecordHelper INSTANCE = new NonoRecordHelper();
    }

    /**
     * 这个正在录制的状态，是从点击录制按钮开始录制，到发布分享页的整个流程
     * 录制进度条在显示，生成视频的进度条对话框和发布页的对话框在显示时，都是true
     * 其他为false
     *
     * @return
     */
    public boolean isRecordingLive() {
        return isRecordingLive;
    }

    public void setRecordingLive(boolean isRecordingLive) {
        this.isRecordingLive = isRecordingLive;
    }

    public boolean isRtmpPusherRecording() {
        return isRtmpPusherRecording;
    }

    public void setRtmpPusherRecording(boolean isRtmpPusherRecording) {
        this.isRtmpPusherRecording = isRtmpPusherRecording;
    }

    public static long getIjkRecordStartTime(Context context) {
        return (long) SharedPreferencesUtil.get(context, SP_VIDEO_RENDERING_START_TIME, System.currentTimeMillis());
    }

    /**
     * 重置直播间开始播放rtmp流的时间
     *
     * @param context
     * @param time
     */
    public static void resetIjkRecordStartTime(Context context, long time) {
        SharedPreferencesUtil.put(context, SP_VIDEO_RENDERING_START_TIME, time);
    }

    public static long getStartRecordPushTime(Context context) {
        return (long) SharedPreferencesUtil.get(context, SP_START_RECORD_PUSH_TIME, System.currentTimeMillis());
    }

    /**
     * 重置直播间开始录制的时间
     *
     * @param context
     * @param time
     */
    public static void resetStartRecordPushTime(Context context, long time) {
        SharedPreferencesUtil.put(context, SP_START_RECORD_PUSH_TIME, time);
    }

    public static final String SP_START_LIVE_TIME = "SP_START_LIVE_TIME";

    public static long getGameLiveStartTime(Context context) {
        return (long) SharedPreferencesUtil.get(context, SP_START_LIVE_TIME, System.currentTimeMillis());
    }

    public static void resetGameLiveStartTime(Context context, long time) {
        SharedPreferencesUtil.put(context, SP_START_LIVE_TIME, time);
    }

    public static long getOrientationChangedTime(Context context) {
        return (long) SharedPreferencesUtil.get(context, SP_ORIENTATION_CHANGED_TIME, System.currentTimeMillis());
    }

    public static void resetOrientationChangedTime(Context context, long time) {
        SharedPreferencesUtil.put(context, SP_ORIENTATION_CHANGED_TIME, time);
    }
}
