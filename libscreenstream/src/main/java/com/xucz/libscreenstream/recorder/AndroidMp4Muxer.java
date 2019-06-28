package com.xucz.libscreenstream.recorder;

import android.annotation.TargetApi;
import android.media.MediaMuxer;
import android.util.Log;

import com.xucz.libscreenstream.log.PushLog;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
@TargetApi(18)
public class AndroidMp4Muxer extends MultiMp4Muxer {
    private static final String TAG = AndroidMp4Muxer.class.getSimpleName() + "dq";
    private MediaMuxer mMediaMuxer;
    private int videoTrackIndex;
    private int audioTrackIndex;
    private long startVideoTime = 0L;
    private long startAudioTime = 0L;
    private long lastAudioTime = 0L;

    public AndroidMp4Muxer(RecordHandler recordHandler, RecordConfig recordConfig) {
        super(recordHandler, recordConfig);
    }

    public synchronized boolean initMuxer() {
        try {
            if (this.mRecFile == null) {
                return false;
            } else {
                String outMp4Path = this.mRecFile.getAbsolutePath();
                this.mMediaMuxer = new MediaMuxer(outMp4Path, 0);
                this.videoTrackIndex = this.mMediaMuxer.addTrack(this.videoFormat);
                if (this.isWriteAudioData) {
                    this.audioTrackIndex = this.mMediaMuxer.addTrack(this.audioFormat);
                }

                this.startVideoTime = 0L;
                this.startAudioTime = 0L;
                this.mMediaMuxer.start();
                return true;
            }
        } catch (Exception var2) {
            PushLog.d(TAG, "initAndroidMuxer error=" + var2);
            return false;
        }
    }

    public synchronized void muxerFrame2Mp4(SrsEsFrame frame) {
        if (frame != null) {
            try {
                if (frame.is_video()) {
                    if (0L == this.startVideoTime) {
                        this.startVideoTime = frame.bi.presentationTimeUs;
                    }

                    frame.bi.presentationTimeUs = (frame.bi.presentationTimeUs - this.startVideoTime) * 1000L;
                    this.mMediaMuxer.writeSampleData(this.videoTrackIndex, frame.bb, frame.bi);
                } else if (frame.is_audio()) {
                    if (0L == this.startAudioTime) {
                        this.startAudioTime = frame.bi.presentationTimeUs;
                    }

                    frame.bi.presentationTimeUs = (frame.bi.presentationTimeUs - this.startAudioTime) * 1000L;
                    if (frame.bi.presentationTimeUs < this.lastAudioTime) {
                        frame.bi.presentationTimeUs = this.lastAudioTime + 1000L;
                        Log.e(TAG, "Correct Audio pts, last pts=" + this.lastAudioTime);
                    }

                    this.lastAudioTime = frame.bi.presentationTimeUs;
                    this.mMediaMuxer.writeSampleData(this.audioTrackIndex, frame.bb, frame.bi);
                }
            } catch (Exception var3) {
                PushLog.d(TAG, "Android muxerFrame2Mp4 error=" + var3);
            }

        }
    }

    protected synchronized void stopMuxer() {
        if (this.mMediaMuxer != null) {
            try {
                this.mMediaMuxer.stop();
            } catch (Exception var3) {
                var3.printStackTrace();
            }

            try {
                this.mMediaMuxer.release();
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            this.mMediaMuxer = null;
        }

    }
}
