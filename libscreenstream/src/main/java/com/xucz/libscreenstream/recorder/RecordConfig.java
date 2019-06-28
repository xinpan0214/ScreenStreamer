package com.xucz.libscreenstream.recorder;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class RecordConfig {
    public boolean isEnablePushScreen;
    public boolean isEnableRecordLiveStream;
    public int recordIntervalTime;
    public String recordVideoSuffix;

    public RecordConfig(boolean isEnablePushScreen, boolean isEnableRecordLiveStream) {
        this.isEnablePushScreen = false;
        this.isEnableRecordLiveStream = false;
        this.recordIntervalTime = 2;
        this.recordVideoSuffix = ".mp4";
        this.isEnablePushScreen = isEnablePushScreen;
        this.isEnableRecordLiveStream = isEnableRecordLiveStream;
    }

    public RecordConfig(boolean isEnablePushScreen, boolean isEnableRecordLiveStream, int recordIntervalTime, String recordVideoSuffix) {
        this(isEnablePushScreen, isEnableRecordLiveStream);
        this.recordIntervalTime = recordIntervalTime;
        this.recordVideoSuffix = recordVideoSuffix;
    }

    public RecordConfig setEnablePushScreen(boolean enablePushScreen) {
        this.isEnablePushScreen = enablePushScreen;
        return this;
    }

    public RecordConfig setEnableRecordLiveStream(boolean enableRecordLiveStream) {
        this.isEnableRecordLiveStream = enableRecordLiveStream;
        return this;
    }

    public RecordConfig setRecordIntervalTime(int recordIntervalTime) {
        this.recordIntervalTime = recordIntervalTime;
        return this;
    }

    public RecordConfig setRecordVideoSuffix(String recordVideoSuffix) {
        this.recordVideoSuffix = recordVideoSuffix;
        return this;
    }
}
