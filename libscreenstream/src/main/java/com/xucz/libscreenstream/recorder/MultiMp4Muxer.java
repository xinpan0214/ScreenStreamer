package com.xucz.libscreenstream.recorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.utils.FileUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public abstract class MultiMp4Muxer extends BaseMp4Muxer {
    private static final String TAG = MultiMp4Muxer.class.getSimpleName() + " dq-";
    protected MediaFormat videoFormat = null;
    protected MediaFormat audioFormat = null;
    protected File mRecFile;
    private Thread worker;
    public volatile boolean isRecording = false;
    private final Object writeLock = new Object();
    private ConcurrentLinkedQueue<SrsEsFrame> frameCache = new ConcurrentLinkedQueue();
    private String parentDir = null;
    private boolean isPaused = false;
    public static final int VIDEO_TRACK = 10;
    public static final int AUDIO_TRACK = 11;
    private MultiMp4Muxer.SrsRawH264Stream avc = new MultiMp4Muxer.SrsRawH264Stream();
    public boolean aacSpecConfig = false;
    public ByteBuffer h264_sps = null;
    public ByteBuffer h264_pps = null;
    public ArrayList<byte[]> spsList = new ArrayList();
    public ArrayList<byte[]> ppsList = new ArrayList();
    public volatile boolean needToFindKeyFrame = true;
    public int videoKeyframeCheckNum = 0;
    private volatile long startTime = 0L;
    public volatile boolean isStartNewRecord = false;

    public MultiMp4Muxer(RecordHandler recordHandler, RecordConfig recordConfig) {
        super(recordHandler, recordConfig);
    }

    public synchronized boolean record(String filePath, int orientation) {
        this.mOrientation = orientation;
        return this.record(filePath);
    }

    public synchronized boolean record(String filePath) {
        this.filePath = filePath;
        this.isRecording = false;
        this.isRecordByKeyFrame = !TextUtils.isEmpty(filePath) && filePath.endsWith(File.separator);
        File outputFile = new File(filePath);
        if (this.isRecordByKeyFrame) {
            this.parentDir = filePath;
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            this.createThreadTask();
            this.isRecording = true;
        } else {
            this.needToFindKeyFrame = true;
            FileUtils.createFile(outputFile.getAbsolutePath());
            this.mRecFile = outputFile;
            this.isRecording = this.startRecord();
        }

        return this.isRecording;
    }

    private boolean startRecord() {
        if (this.videoFormat == null) {
            return false;
        } else if (this.isWriteAudioData && this.audioFormat == null) {
            return false;
        } else if (!this.spsList.isEmpty() && !this.ppsList.isEmpty()) {
            if (this.isRecording) {
                this.stop();
            }

            if (!this.initMuxer()) {
                return false;
            } else {
                this.createThreadTask();
                if (!this.isRecordByKeyFrame && this.mHandler != null) {
                    this.mHandler.notifyRecordStarted(this.mRecFile.getPath());
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private void createThreadTask() {
        if (this.worker == null) {
            this.worker = new Thread(new Runnable() {
                public void run() {
                    MultiMp4Muxer.this.recordingVideo();
                }
            });
            this.worker.start();
        }
    }

    private void recordingVideo() {
        this.isRecording = true;

        while (this.isRecording) {
            while (!this.frameCache.isEmpty()) {
                MultiMp4Muxer.SrsEsFrame frame = (MultiMp4Muxer.SrsEsFrame) this.frameCache.poll();
                if (frame != null) {
                    this.muxerFrame2Mp4(frame);
                }
            }

            synchronized (this.writeLock) {
                try {
                    this.writeLock.wait(10L);
                } catch (InterruptedException var4) {
                    PushLog.d(TAG, "writeLock.wait error " + var4);
                    if (this.worker != null) {
                        this.worker.interrupt();
                    }
                }
            }
        }

    }

    public abstract boolean initMuxer();

    public abstract void muxerFrame2Mp4(MultiMp4Muxer.SrsEsFrame var1);

    public void pause() {
        this.isPaused = true;
        this.frameCache.clear();
        if (this.isRecordByKeyFrame) {
            this.stopCurrentTask();
        }

    }

    public void resume() {
        this.isPaused = false;
    }

    public synchronized void stop() {
        this.isRecording = false;
        this.needToFindKeyFrame = true;
        this.frameCache.clear();
        if (this.worker != null) {
            try {
                this.worker.join(20L);
            } catch (InterruptedException var2) {
                PushLog.d(TAG, "writeLock.join error " + var2);
                this.worker.interrupt();
            }

            this.worker = null;
            this.stopCurrentTask();
        }

    }

    public void stopCurrentTask() {
        this.isStartNewRecord = false;
        this.frameCache.clear();
        this.stopMuxer();
        if (this.mHandler != null) {
            this.mHandler.notifyRecordException((Exception) null, this.filePath);
        }

    }

    protected abstract void stopMuxer();

    public synchronized void storeMediaFormat(int type, MediaFormat format, byte[] sps, byte[] pps) {
        if (format != null) {
            if (10 == type) {
                if (this.videoFormat == null) {
                    this.addSPS(sps);
                    this.addPPS(pps);
                } else {
                    boolean isSpsChanged = this.addSPS(sps);
                    boolean isPpsChanged = this.addPPS(pps);
                    if (isSpsChanged || isPpsChanged) {
                        this.frameCache.clear();
                    }

                    this.resume();
                }

                this.videoFormat = format;
            } else if (11 == type) {
                this.audioFormat = format;
            }

        }
    }

    public synchronized void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        boolean isVideo = 10 == trackIndex;
        if (isVideo) {
            this.writeVideoSample(byteBuf, bufferInfo);
        } else {
            this.writeAudioSample(byteBuf, bufferInfo);
        }

    }

    private synchronized void writeVideoSample(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (bb != null && bi != null) {
            if (!this.spsList.isEmpty() && !this.ppsList.isEmpty()) {
                this.notifyRecordPrepared();
                boolean isKeyFrame = (bi.flags & 1) != 0;
                this.writeFrameByte(10, bb, bi, isKeyFrame);
            }

        }
    }

    public boolean addPPS(byte[] pps) {
        ByteBuffer newPPS = ByteBuffer.wrap(pps);
        if (this.h264_pps != null && this.h264_pps.equals(newPPS)) {
            PushLog.d("MultiMp4Muxer pps same with before");
            return true;
        } else {
            this.h264_pps = newPPS;
            if (this.ppsList != null) {
                this.ppsList.clear();
                this.ppsList.add(pps);
            }

            return false;
        }
    }

    public boolean addSPS(byte[] sps) {
        ByteBuffer newSPS = ByteBuffer.wrap(sps);
        if (this.h264_sps != null && this.h264_sps.equals(newSPS)) {
            PushLog.d("MultiMp4Muxer sps same with before");
            return true;
        } else {
            this.h264_sps = newSPS;
            if (this.spsList != null) {
                this.spsList.clear();
                this.spsList.add(sps);
            }

            return false;
        }
    }

    private void notifyRecordPrepared() {
        if (!this.isPrepared) {
            if (!this.isWriteAudioData || this.audioFormat != null) {
                if (this.videoFormat != null) {
                    this.isPrepared = true;
                    if (this.mHandler != null) {
                        this.mHandler.notifyRecordPrepared();
                    }
                }

            }
        }
    }

    private void writeAudioSample(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if (!this.aacSpecConfig) {
            this.aacSpecConfig = true;
        } else {
            this.writeFrameByte(11, bb, bi, false);
        }

    }

    private void writeFrameByte(int track, ByteBuffer bb, MediaCodec.BufferInfo bi, boolean isKeyFrame) {
        if (this.isRecording && !this.isPaused) {
            MultiMp4Muxer.SrsEsFrame frame = new MultiMp4Muxer.SrsEsFrame();
            frame.bb = bb;
            frame.bi = bi;
            frame.isKeyFrame = isKeyFrame;
            frame.track = track;
            if (frame.is_video()) {
                if (frame.isKeyFrame) {
                    this.videoKeyframeCheckNum = 0;
                } else {
                    ++this.videoKeyframeCheckNum;
                    if (this.videoKeyframeCheckNum == 64) {
                        this.videoKeyframeCheckNum = 100;
                        if (this.mHandler != null) {
                            this.mHandler.notifyRecordNoKeyframe();
                        }

                        PushLog.d(TAG, "notifyRecord NoKeyframe =========" + this.videoKeyframeCheckNum);
                    }
                }
            }

            if (!this.isRecordByKeyFrame) {
                if (this.needToFindKeyFrame) {
                    if (frame.isKeyFrame) {
                        this.needToFindKeyFrame = false;
                        PushLog.d(TAG, "key frame=========");
                        this.handleFrame(frame);
                    }
                } else {
                    this.handleFrame(frame);
                }
            } else if (frame.isKeyFrame) {
                long presentationTimeUs = frame.bi.presentationTimeUs;
                long timeUnit = 1000000L;
                long duration = presentationTimeUs - this.startTime;
                Log.e("MPEG4Writer", "duration: " + duration + ", " + (long) this.recordIntervalTime * timeUnit);
                if (duration <= (long) this.recordIntervalTime * timeUnit && this.isStartNewRecord) {
                    this.handleFrame(frame);
                    return;
                }

                this.isStartNewRecord = this.startNewRecord();
                if (this.isStartNewRecord) {
                    this.startTime = presentationTimeUs;
                    this.handleFrame(frame);
                    PushLog.d(TAG, duration + "，startNewRecord key frame=========" + this.startTime);
                } else {
                    PushLog.d(TAG, "startNewRecord====failed=====");
                }
            } else if (this.isStartNewRecord) {
                this.handleFrame(frame);
            }
        }

    }

    public boolean startNewRecord() {
        if (TextUtils.isEmpty(this.parentDir)) {
            return false;
        } else {
            if (!this.parentDir.endsWith(File.separator)) {
                this.parentDir = this.parentDir + File.separator;
            }

            this.filePath = this.parentDir + "nonolive-" + System.currentTimeMillis() + this.recordVideoSuffix;
            FileUtils.createFile(this.filePath);
            this.mRecFile = new File(this.filePath);
            this.stopCurrentTask();
            if (!this.initMuxer()) {
                return false;
            } else {
                this.createThreadTask();
                if (this.mHandler != null) {
                    this.mHandler.notifyRecordStarted(this.filePath);
                }

                return true;
            }
        }
    }

    private void handleFrame(MultiMp4Muxer.SrsEsFrame frame) {
        this.frameCache.add(frame);
        synchronized (this.writeLock) {
            this.writeLock.notifyAll();
        }
    }

    private class SrsAvcNaluType {
        public static final int Reserved = 0;
        public static final int NonIDR = 1;
        public static final int DataPartitionA = 2;
        public static final int DataPartitionB = 3;
        public static final int DataPartitionC = 4;
        public static final int IDR = 5;
        public static final int SEI = 6;
        public static final int SPS = 7;
        public static final int PPS = 8;
        public static final int AccessUnitDelimiter = 9;
        public static final int EOSequence = 10;
        public static final int EOStream = 11;
        public static final int FilterData = 12;
        public static final int SPSExt = 13;
        public static final int PrefixNALU = 14;
        public static final int SubsetSPS = 15;
        public static final int LayerWithoutPartition = 19;
        public static final int CodedSliceExt = 20;

        private SrsAvcNaluType() {
        }
    }

    public class SrsEsFrame {
        public ByteBuffer bb;
        public MediaCodec.BufferInfo bi;
        public int track;
        public boolean isKeyFrame;

        public SrsEsFrame() {
        }

        public boolean is_video() {
            return this.track == 10;
        }

        public boolean is_audio() {
            return this.track == 11;
        }
    }

    private class SrsEsFrameBytes {
        public ByteBuffer data;
        public int size;

        private SrsEsFrameBytes() {
        }
    }

    private class SrsAnnexbSearch {
        public int nb_start_code;
        public boolean match;

        private SrsAnnexbSearch() {
            this.nb_start_code = 0;
            this.match = false;
        }
    }

    private class SrsRawH264Stream {
        private SrsRawH264Stream() {
        }

        public boolean is_sps(MultiMp4Muxer.SrsEsFrameBytes frame) {
            if (frame.size < 1) {
                return false;
            } else {
                return (frame.data.get(0) & 31) == 7;
            }
        }

        public boolean is_pps(MultiMp4Muxer.SrsEsFrameBytes frame) {
            if (frame.size < 1) {
                return false;
            } else {
                return (frame.data.get(0) & 31) == 8;
            }
        }

        public MultiMp4Muxer.SrsAnnexbSearch srs_avc_startswith_annexb(ByteBuffer bb, MediaCodec.BufferInfo bi) {
            MultiMp4Muxer.SrsAnnexbSearch as = MultiMp4Muxer.this.new SrsAnnexbSearch();
            as.match = false;

            for (int pos = bb.position(); pos < bi.size - 3 && bb.get(pos) == 0 && bb.get(pos + 1) == 0; ++pos) {
                if (bb.get(pos + 2) == 1) {
                    as.match = true;
                    as.nb_start_code = pos + 3 - bb.position();
                    break;
                }
            }

            return as;
        }

        public MultiMp4Muxer.SrsEsFrameBytes annexb_demux(ByteBuffer bb, MediaCodec.BufferInfo bi) {
            MultiMp4Muxer.SrsEsFrameBytes tbb = MultiMp4Muxer.this.new SrsEsFrameBytes();
            if (bb.position() < bi.size) {
                MultiMp4Muxer.SrsAnnexbSearch tbbsc = this.srs_avc_startswith_annexb(bb, bi);
                if (!tbbsc.match || tbbsc.nb_start_code < 3) {
                    PushLog.d(MultiMp4Muxer.TAG, "annexb not match.");
                    if (MultiMp4Muxer.this.mHandler != null) {
                        MultiMp4Muxer.this.mHandler.notifyRecordException(new IllegalArgumentException(String.format(Locale.getDefault(), "annexb not match for %dB, pos=%d", bi.size, bb.position())), MultiMp4Muxer.this.filePath);
                    }
                }

                ByteBuffer tbbs = bb.slice();

                int pos;
                for (pos = 0; pos < tbbsc.nb_start_code; ++pos) {
                    bb.get();
                }

                tbb.data = bb.slice();
                pos = bb.position();

                while (bb.position() < bi.size) {
                    MultiMp4Muxer.SrsAnnexbSearch bsc = this.srs_avc_startswith_annexb(bb, bi);
                    if (bsc.match) {
                        break;
                    }

                    bb.get();
                }

                tbb.size = bb.position() - pos;
            }

            return tbb;
        }
    }
}
