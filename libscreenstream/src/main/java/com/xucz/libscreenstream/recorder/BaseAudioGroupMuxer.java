package com.xucz.libscreenstream.recorder;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.xucz.libscreenstream.log.PushLog;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class BaseAudioGroupMuxer extends BaseMp4Muxer {
    private List<BaseMp4Muxer> group = new ArrayList();
    private MediaFormat audioFormat;

    public BaseAudioGroupMuxer() {
        super((RecordHandler) null, (RecordConfig) null);
    }

    public void add(BaseMp4Muxer muxer) {
        this.group.add(muxer);
        if (null != this.audioFormat) {
            muxer.storeMediaFormat(11, this.audioFormat, (byte[]) null, (byte[]) null);
        }

    }

    public void remove(BaseMp4Muxer muxer) {
        this.group.remove(muxer);
    }

    public void clear() {
        this.group.clear();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean record(String filePath, int orientation) {
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean record(String filePath) {
        return false;
    }

    public void stop() {
        Iterator var1 = this.group.iterator();

        while (var1.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var1.next();
            m.stop();
        }

    }

    public void storeMediaFormat(int typeVideo, MediaFormat mediaFormat, byte[] sps, byte[] pps) {
        if (typeVideo == 11) {
            this.audioFormat = mediaFormat;
        }

        Iterator var5 = this.group.iterator();

        while (var5.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var5.next();
            if (typeVideo == 11) {
                m.storeMediaFormat(typeVideo, this.audioFormat, sps, pps);
            } else {
                m.storeMediaFormat(typeVideo, mediaFormat, sps, pps);
            }
        }

    }

    public void writeSampleData(int dataType, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        Iterator var4 = this.group.iterator();

        while (var4.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var4.next();
            m.writeSampleData(dataType, byteBuf, bufferInfo);
            if (11 == dataType) {
                PushLog.e("writeSampleData: " + bufferInfo.presentationTimeUs);
            }
        }

    }

    public void stopCurrentTask() {
        Iterator var1 = this.group.iterator();

        while (var1.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var1.next();
            m.stopCurrentTask();
        }

    }

    public void pause() {
        Iterator var1 = this.group.iterator();

        while (var1.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var1.next();
            m.pause();
        }

    }

    public void resume() {
        Iterator var1 = this.group.iterator();

        while (var1.hasNext()) {
            BaseMp4Muxer m = (BaseMp4Muxer) var1.next();
            m.resume();
        }

    }
}
