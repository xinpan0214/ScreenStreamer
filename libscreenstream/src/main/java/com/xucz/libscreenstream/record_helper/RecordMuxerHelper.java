package com.xucz.libscreenstream.record_helper;

import com.xucz.libscreenstream.recorder.BaseAudioGroupMuxer;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;

/**
 * Description:秀场开播共用音频录制，统一管理muxer
 *
 * @author Dusan, Created on 2018/8/20 - 20:30.
 * E-mail:duqian2010@gmail.com
 */
public class RecordMuxerHelper {
    private BaseAudioGroupMuxer baseAudioGroupMuxer = new BaseAudioGroupMuxer();

    public void add(BaseMp4Muxer muxer) {
        this.baseAudioGroupMuxer.add(muxer);
    }

    public void remove(BaseMp4Muxer muxer) {
        this.baseAudioGroupMuxer.remove(muxer);
    }

    public void clear() {
        this.baseAudioGroupMuxer.clear();
    }

    public BaseAudioGroupMuxer getBaseAudioGroupMuxer() {
        return baseAudioGroupMuxer;
    }

    public static RecordMuxerHelper get() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static RecordMuxerHelper INSTANCE = new RecordMuxerHelper();
    }

}
