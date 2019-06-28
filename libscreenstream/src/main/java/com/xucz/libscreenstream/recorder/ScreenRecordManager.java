package com.xucz.libscreenstream.recorder;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class ScreenRecordManager {
    private BaseMp4Muxer baseMp4Muxer;
    public RecordHandler mHandler;
    public RecordConfig mConfig;

    public static ScreenRecordManager getPushRoomRecorder(boolean isEnableRecordLiveStream) {
        RecordConfig config = new RecordConfig(false, isEnableRecordLiveStream);
        return new ScreenRecordManager(config);
    }

    public static ScreenRecordManager getLiveRoomRecorder() {
        RecordConfig config = new RecordConfig(false, false);
        return new ScreenRecordManager(config);
    }

    public static ScreenRecordManager getMobileGameLiveRecorder() {
        RecordConfig config = new RecordConfig(true, false);
        return new ScreenRecordManager(config);
    }

    public ScreenRecordManager(RecordConfig config) {
        this.mConfig = config;
        this.initMp4Muxer();
    }

    private void initMp4Muxer() {
        this.baseMp4Muxer = new Mp4ParserMuxer(this.mHandler, this.mConfig);
    }

    public void setRecordHandler(RecordHandler mHandler) {
        this.mHandler = mHandler;
        this.baseMp4Muxer.setRecordHandler(mHandler);
    }

    public boolean isEnablePushScreen() {
        return this.mConfig != null && this.mConfig.isEnablePushScreen;
    }

    public boolean isEnableRecordLiveStream() {
        return this.mConfig != null && this.mConfig.isEnableRecordLiveStream;
    }

    public void setEnableRecordAudio(boolean enableRecordAudio) {
        if (this.baseMp4Muxer != null) {
            this.baseMp4Muxer.setWriteAudioData(enableRecordAudio);
        }

    }

    public void setRecordIntervalTime(int recordIntervalTime) {
        if (this.baseMp4Muxer != null) {
            this.baseMp4Muxer.setRecordIntervalTime(recordIntervalTime);
        }

    }

    public void setRecordVideoSuffix(String recordVideoSuffix) {
        if (this.baseMp4Muxer != null) {
            this.baseMp4Muxer.setRecordVideoSuffix(recordVideoSuffix);
        }

    }

    public BaseMp4Muxer getBaseMp4Muxer() {
        return this.baseMp4Muxer;
    }

    public boolean startRecord(String filePath, int orientation) {
        return this.baseMp4Muxer == null ? false : this.baseMp4Muxer.record(filePath, orientation);
    }

    public boolean startRecord(String filePath) {
        return this.startRecord(filePath, 0);
    }

    public void stopRecord() {
        if (this.baseMp4Muxer != null) {
            this.baseMp4Muxer.stop();
        }
    }

    public void setOrientation(int mOrientation) {
        if (this.baseMp4Muxer != null) {
            this.baseMp4Muxer.setOrientation(mOrientation);
        }
    }
}
