package com.xucz.libscreenstream.recorder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public abstract class BaseMp4Muxer {
    public boolean isRecordByKeyFrame = false;
    public static final int TYPE_VIDEO = 10;
    public static final int TYPE_AUDIO = 11;
    public String filePath;
    public boolean isWriteAudioData = true;
    public boolean isPrepared = false;
    public int recordIntervalTime = 2;
    public String recordVideoSuffix = ".mp4";
    public int mOrientation = 0;
    public boolean isOffset = true;
    public RecordHandler mHandler;
    public RecordConfig recordConfig;

    public BaseMp4Muxer(RecordHandler mHandler, RecordConfig recordConfig) {
        this.mHandler = mHandler;
        this.recordConfig = recordConfig;
        if (recordConfig != null) {
            this.recordIntervalTime = recordConfig.recordIntervalTime;
            this.recordVideoSuffix = recordConfig.recordVideoSuffix;
        }

    }

    public void setWriteAudioData(boolean writeAudioData) {
        this.isWriteAudioData = writeAudioData;
    }

    public void setRecordIntervalTime(int recordIntervalTime) {
        this.recordIntervalTime = recordIntervalTime;
    }

    public void setRecordVideoSuffix(String recordVideoSuffix) {
        this.recordVideoSuffix = recordVideoSuffix;
    }

    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public void setOffset(boolean offset) {
        this.isOffset = offset;
    }

    public abstract boolean record(String var1, int var2);

    public abstract boolean record(String var1);

    public abstract void stop();

    public abstract void writeSampleData(int var1, ByteBuffer var2, MediaCodec.BufferInfo var3);

    public void setRecordHandler(RecordHandler mHandler) {
        this.mHandler = mHandler;
    }

    public boolean addPPS(byte[] pps) {
        return false;
    }

    public boolean addSPS(byte[] sps) {
        return false;
    }

    public abstract void stopCurrentTask();

    public abstract void pause();

    public abstract void resume();

    public void storeMediaFormat(int typeVideo, MediaFormat mediaFormat, byte[] sps, byte[] pps) {
    }
}
