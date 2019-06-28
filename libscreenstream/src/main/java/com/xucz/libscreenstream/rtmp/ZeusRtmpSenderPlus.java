package com.xucz.libscreenstream.rtmp;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import com.xucz.libscreenstream.callback.IConnectionListener;
import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.pusher.EventDispatcher;
import com.xucz.libscreenstream.utils.FrameRateMeter;
import com.xucz.libscreenstream.utils.StringUtils;

import java.util.concurrent.LinkedBlockingQueue;

public class ZeusRtmpSenderPlus {
    private ZeusRtmpSenderPlus.RtmpHandler rtmpHandler;
    private HandlerThread rtmpHandlerThread;
    private boolean isVideoReady = false;
    private boolean isAudioReady = false;
    private long startTime = 0L;
    private int lastPts;

    public ZeusRtmpSenderPlus() {
    }

    public void prepare(Configure configure) {
        this.rtmpHandlerThread = new HandlerThread("RtmpSenderThread");
        this.rtmpHandlerThread.start();
        this.rtmpHandler = new ZeusRtmpSenderPlus.RtmpHandler(this.rtmpHandlerThread.getLooper(), configure);
    }

    public void setConnectionListener(IConnectionListener connectionListener) {
        this.rtmpHandler.setConnectionListener(connectionListener);
    }

    public void start(String rtmpAddr) {
        this.rtmpHandler.sendStart(rtmpAddr);
    }

    public void sendVideoSequenceHeader(byte[] spsData, int spsSize, byte[] ppsData, int ppsSize, boolean softEncoder) {
        this.rtmpHandler.sendVideoSequenceHeader(new ZeusRtmpSenderPlus.VideoSequenceHeaderInfo(spsData, spsSize, ppsData, ppsSize), softEncoder);
    }

    public void clearSpsPps() {
        this.rtmpHandler.clearSpsPps();
    }

    private void resetState() {
        this.isVideoReady = false;
        this.isAudioReady = false;
        this.startTime = 0L;
        this.lastPts = 0;
    }

    public synchronized void sendVideoPacket(byte[] videoData, int videoSize, int timeoffset, boolean isKeyframe) {
        this.isVideoReady = true;
        if (this.isAudioReady) {
            if (this.startTime <= 0L) {
                this.startTime = System.currentTimeMillis();
            }

            timeoffset = (int) (System.currentTimeMillis() - this.startTime);
            if (timeoffset < this.lastPts) {
                PushLog.e("Video pts " + timeoffset + " is smaller than " + this.lastPts);
            }

            ZeusRtmpSenderPlus.StreamInfo info = new ZeusRtmpSenderPlus.StreamInfo(new ZeusRtmpSenderPlus.VideoPacketInfo(videoData, videoSize, timeoffset, isKeyframe));
            this.rtmpHandler.cachePackageBuffer(info);
            this.lastPts = timeoffset;
        }
    }

    public void sendAudioSequenceHeader() {
        this.rtmpHandler.sendAudioSequenceHeader();
    }

    public synchronized void sendAudioPacket(byte[] audioData, int audioSize, int timeoffset, boolean hasHeader) {
        this.isAudioReady = true;
        if (this.isVideoReady) {
            if (this.startTime <= 0L) {
                this.startTime = System.currentTimeMillis();
            }

            timeoffset = (int) (System.currentTimeMillis() - this.startTime);
            if (timeoffset < this.lastPts) {
                PushLog.i("Audio pts " + timeoffset + " is smaller than " + this.lastPts);
            }

            ZeusRtmpSenderPlus.StreamInfo info = new ZeusRtmpSenderPlus.StreamInfo(new ZeusRtmpSenderPlus.AudioPacketInfo(audioData, audioSize, timeoffset, hasHeader));
            this.rtmpHandler.cachePackageBuffer(info);
            this.lastPts = timeoffset;
        }
    }

    public int getTotalSpeed() {
        return this.rtmpHandler != null ? this.rtmpHandler.getTotalSpeed() : 0;
    }

    public float getVideoTransFps() {
        return this.rtmpHandler != null ? this.rtmpHandler.getVideoSendFps() : 0.0F;
    }

    public ZeusRtmpSenderPlus.StatisticsBandwidth getBandwidthStatisticsAndRestart() {
        return this.rtmpHandler != null ? this.rtmpHandler.getBandwidthStatisticsAndRestart() : null;
    }

    public boolean isRtmpConnected() {
        return this.rtmpHandler != null ? this.rtmpHandler.isRtmpConnected() : false;
    }

    public void stop(boolean isLiveEnd) {
        if (this.rtmpHandler != null) {
            this.rtmpHandler.sendStop(isLiveEnd);
        }

    }

    public void destroy() {
        this.resetState();
        if (this.rtmpHandler != null) {
            this.rtmpHandler.destroy();
        }

        if (this.rtmpHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                this.rtmpHandlerThread.quitSafely();
            } else {
                this.rtmpHandlerThread.quit();
            }
        }

        this.rtmpHandler = null;
        this.rtmpHandlerThread = null;
    }

    public void disConnectRtmp() {
        if (this.rtmpHandler != null) {
            this.rtmpHandler.disConnectRtmp();
        }

    }

    public void reconnectRtmp() {
        if (this.rtmpHandler != null) {
            this.rtmpHandler.reconnectRtmp();
        }

    }

    public void reSendRtmpFlvHeader(boolean softMode) {
        if (this.rtmpHandler != null) {
            this.rtmpHandler.reSendRtmpFlvHeader(softMode);
        }

    }

    public class StatisticsBandwidth implements Cloneable {
        private int totalProcessCount = 0;
        private int smoothRaiseCount = 0;
        private int trasherDownCount = 0;
        private int slowDropCount = 0;
        private int rapidDropCount = 0;

        public StatisticsBandwidth() {
        }

        public int getTotalProcessCount() {
            return this.totalProcessCount;
        }

        public void setTotalProcessCount(int totalProcessCount) {
            this.totalProcessCount = totalProcessCount;
        }

        public int getSmoothRaiseCount() {
            return this.smoothRaiseCount;
        }

        public void setSmoothRaiseCount(int smoothRaiseCount) {
            this.smoothRaiseCount = smoothRaiseCount;
        }

        public int getTrasherDownCount() {
            return this.trasherDownCount;
        }

        public void setTrasherDownCount(int trasherDownCount) {
            this.trasherDownCount = trasherDownCount;
        }

        public int getSlowDropCount() {
            return this.slowDropCount;
        }

        public void setSlowDropCount(int slowDropCount) {
            this.slowDropCount = slowDropCount;
        }

        public int getRapidDropCount() {
            return this.rapidDropCount;
        }

        public void setRapidDropCount(int rapidDropCount) {
            this.rapidDropCount = rapidDropCount;
        }

        public void restart() {
            this.totalProcessCount = 0;
            this.smoothRaiseCount = 0;
            this.trasherDownCount = 0;
            this.slowDropCount = 0;
            this.rapidDropCount = 0;
        }

        public Object clone() {
            ZeusRtmpSenderPlus.StatisticsBandwidth statisticsBandwidth = null;

            try {
                statisticsBandwidth = (ZeusRtmpSenderPlus.StatisticsBandwidth) super.clone();
            } catch (CloneNotSupportedException var3) {
                var3.printStackTrace();
            }

            return statisticsBandwidth;
        }
    }

    public interface BitrateUpdateType {
        int SMOOTH_UP = 1;
        int SMOOTH_DOWN = 2;
        int DROP_FRAME_DOWN = 3;
    }

    class AudioPacketInfo {
        public byte[] audioData;
        public int audioSize;
        public int timeoffset;
        public boolean hasHeader;

        public AudioPacketInfo(byte[] audioData, int audioSize, int timeoffset, boolean hasHeader) {
            this.audioData = audioData;
            this.audioSize = audioSize;
            this.hasHeader = hasHeader;
            this.timeoffset = timeoffset;
        }
    }

    class VideoPacketInfo {
        public byte[] videoData;
        public int videoSize;
        public int timeoffset;
        public boolean isKeyframe;

        public VideoPacketInfo(byte[] videoData, int videoSize, int timeoffset, boolean isKeyframe) {
            this.isKeyframe = isKeyframe;
            this.timeoffset = timeoffset;
            this.videoData = videoData;
            this.videoSize = videoSize;
        }
    }

    class StreamInfo implements Comparable<ZeusRtmpSenderPlus.StreamInfo> {
        public ZeusRtmpSenderPlus.VideoPacketInfo videoPacketInfo;
        public ZeusRtmpSenderPlus.AudioPacketInfo audioPacketInfo;
        public boolean isVideoType = false;
        public int dataSize;

        public StreamInfo(ZeusRtmpSenderPlus.AudioPacketInfo audioPacketInfo) {
            this.audioPacketInfo = audioPacketInfo;
            this.isVideoType = false;
            this.videoPacketInfo = null;
            this.dataSize = audioPacketInfo.audioSize;
        }

        public StreamInfo(ZeusRtmpSenderPlus.VideoPacketInfo videoPacketInfo) {
            this.audioPacketInfo = null;
            this.isVideoType = true;
            this.videoPacketInfo = videoPacketInfo;
            this.dataSize = videoPacketInfo.videoSize;
        }

        public int getTimeOffset() {
            return this.isVideoType ? this.videoPacketInfo.timeoffset : this.audioPacketInfo.timeoffset;
        }

        public String toString() {
            return this.isVideoType ? String.valueOf(this.videoPacketInfo.timeoffset) : String.valueOf(this.audioPacketInfo.timeoffset);
        }

        public int compareTo(@NonNull ZeusRtmpSenderPlus.StreamInfo o) {
            return Integer.valueOf(this.getTimeOffset()).compareTo(o.getTimeOffset());
        }
    }

    class VideoSequenceHeaderInfo {
        public byte[] spsData;
        public int spsSize;
        public byte[] ppsData;
        public int ppsSize;

        public VideoSequenceHeaderInfo(byte[] spsData, int spsSize, byte[] ppsData, int ppsSize) {
            this.ppsData = ppsData;
            this.ppsSize = ppsSize;
            this.spsData = spsData;
            this.spsSize = spsSize;
        }
    }

    private class RtmpHandler extends Handler {
        private static final int TIMEGRANULARITY = 3000;
        private static final int MSG_START = 1;
        private static final int MSG_STOP = 2;
        private static final int MSG_SEND_VIDEO_SEQUENCE_HEADER = 3;
        private static final int MSG_SEND_AUDIO_SEQUENCE_HEADER = 5;
        private static final int MSG_HANDLE_DATA_PACKAGE = 7;
        private IConnectionListener connectionListener;
        private final Object syncConnectionListener = new Object();
        private int videoPackageErrorNum = 0;
        private int audioPackageErrorNum = 0;
        private int connRetryCount = 0;
        private ZeusRtmpSenderPlus.StatisticsBandwidth statisticsBandwidth = ZeusRtmpSenderPlus.this.new StatisticsBandwidth();
        private ByteSpeedometer videoByteSpeedometer = new ByteSpeedometer(3000);
        private ByteSpeedometer audioByteSpeedometer = new ByteSpeedometer(3000);
        private FrameRateMeter videoSendFrameRateMeter = new FrameRateMeter();
        protected LinkedBlockingQueue<StreamInfo> streamBlockingQueue;
        private Configure configure;
        private int duration_video_count = 0;
        private int continue_trashing_count = 0;
        private int continue_smoothing_count = 0;
        private int pendingDataSize = 0;
        private ZeusRtmpSenderPlus.VideoSequenceHeaderInfo softVideoSequenceHeaderInfo;
        private ZeusRtmpSenderPlus.VideoSequenceHeaderInfo hardVideoSequenceHeaderInfo;
        private ZeusRtmpSenderPlus.STATE state;
        private volatile boolean isClearQueue = true;
        private boolean isAsusMobile = false;
        private int skipVideoFrameCount = 0;
        private long delayMillis = 0L;
        private volatile int isRtmpConnected = -11;

        RtmpHandler(Looper looper, Configure configure) {
            super(looper);
            this.state = ZeusRtmpSenderPlus.STATE.IDLE;
            this.isRtmpConnected = -11;
            this.configure = configure;
            this.initCacheQueue();
        }

        private void initCacheQueue() {
            this.streamBlockingQueue = new LinkedBlockingQueue(1024);
        }

        public synchronized void sendStart(String rtmpAddr) {
            this.removeMessages(1);
            this.removeMessages(7);
            Message msg = this.obtainMessage(1, rtmpAddr);
            this.sendMessage(msg);
        }

        public synchronized void sendStop(boolean isLiveEnd) {
            this.removeMessages(2);
            this.removeMessages(7);
            this.sendMessage(this.obtainMessage(2, isLiveEnd));
        }

        public synchronized void sendVideoSequenceHeader(ZeusRtmpSenderPlus.VideoSequenceHeaderInfo videoSequenceHeaderInfo, boolean softEncoder) {
            PushLog.e("sendVideoSequenceHeader=====");
            if (softEncoder) {
                this.softVideoSequenceHeaderInfo = videoSequenceHeaderInfo;
            } else {
                this.hardVideoSequenceHeaderInfo = videoSequenceHeaderInfo;
            }

            this.removeMessages(3);
            this.sendMessage(this.obtainMessage(3, videoSequenceHeaderInfo));
        }

        public synchronized void sendAudioSequenceHeader() {
            this.removeMessages(5);
            this.sendEmptyMessage(5);
        }

        private void processBandwidth(ZeusRtmpSenderPlus.StreamInfo streamInfo) {
            if (streamInfo.isVideoType) {
                ++this.duration_video_count;
                if (this.duration_video_count >= 10) {
                    this.duration_video_count = 0;
                    int totalSpeed = this.getTotalSpeedByte();
                    if (totalSpeed > 0) {
                        if (this.statisticsBandwidth.getTotalProcessCount() >= 10000) {
                            this.getBandwidthStatisticsAndRestart();
                        }

                        final float delayTime = (float) this.pendingDataSize * 1.0F / (float) totalSpeed;
                        PushLog.d("ZeusRtmpSenderPlus", "buffer_size: " + this.pendingDataSize * 8 / 1024 + ", video base bitrate: " + this.configure.videoBitRate + ", totalSpeed: " + totalSpeed * 8 / 1024 + "kbps, delayTime: " + delayTime + "s, sendFrameRate: " + this.getVideoSendFps() + ", overflowBufferTime: " + this.configure.overflowBufferTime + ", trashingBufferTime: " + this.configure.trashingBufferTime + ", smoothingCountThreshold: " + this.configure.smoothingCountThreshold);
                        PushLog.d("ZeusRtmpSenderPlus video max bitrate=" + this.configure.videoBitRateMax + ",min bitrate=" + this.configure.videoBitRateMin + "，curr bitrate=" + this.configure.videoBitRate);
                        final int newBitRate;
                        if ((double) delayTime > this.configure.overflowBufferTime) {
                            PushLog.d("ZeusRtmpSenderPlus", "drop buffer");
                            this.streamBlockingQueue.clear();
                            this.isClearQueue = true;
                            this.pendingDataSize = 0;
                            if (this.connectionListener != null) {
                                if (this.configure.videoBitRate > this.configure.videoBitRateMin) {
                                    newBitRate = this.configure.videoBitRateMin;
                                    EventDispatcher.getInstance().post(new Runnable() {
                                        public void run() {
                                            RtmpHandler.this.connectionListener.onChangeMediaCodeParamCallback(newBitRate, true, 3, delayTime);
                                        }
                                    });
                                    this.statisticsBandwidth.rapidDropCount++;
                                } else {
                                    this.statisticsBandwidth.slowDropCount++;
                                }
                            }

                            this.continue_trashing_count = 0;
                            this.continue_smoothing_count = 0;
                        } else if ((double) delayTime > this.configure.trashingBufferTime) {
                            ++this.continue_trashing_count;
                            if (this.continue_trashing_count >= this.configure.trashingCountThreshold) {
                                --this.continue_trashing_count;
                                if (this.connectionListener != null && this.configure.videoBitRate >= this.configure.videoBitRateMin + 100000) {
                                    newBitRate = this.configure.videoBitRate - 100000;
                                    PushLog.d("ZeusRtmpSenderPlus", "drop down bitrate to: " + newBitRate);
                                    EventDispatcher.getInstance().post(new Runnable() {
                                        public void run() {
                                            RtmpHandler.this.connectionListener.onChangeMediaCodeParamCallback(newBitRate, false, 2, delayTime);
                                        }
                                    });
                                    this.statisticsBandwidth.trasherDownCount++;
                                }
                            }

                            this.continue_smoothing_count = 0;
                        } else if ((double) delayTime < this.configure.emptyBufferTime) {
                            ++this.continue_smoothing_count;
                            if (this.continue_smoothing_count >= this.configure.smoothingCountThreshold) {
                                if (this.configure.videoBitRate <= this.configure.videoBitRateMax - '썐') {
                                    newBitRate = this.configure.videoBitRate + '썐';
                                    PushLog.d("ZeusRtmpSenderPlus", "grow up bitrate to: " + newBitRate);
                                    if (this.connectionListener != null) {
                                        EventDispatcher.getInstance().post(new Runnable() {
                                            public void run() {
                                                RtmpHandler.this.connectionListener.onChangeMediaCodeParamCallback(newBitRate, false, 1, delayTime);
                                            }
                                        });
                                        this.statisticsBandwidth.smoothRaiseCount++;
                                    }
                                }

                                this.continue_smoothing_count = 0;
                                this.continue_trashing_count = 0;
                            }
                        }

                        this.statisticsBandwidth.totalProcessCount++;
                    }
                }
            }
        }

        public synchronized void cachePackageBuffer(ZeusRtmpSenderPlus.StreamInfo streamInfo) {
            if (this.streamBlockingQueue != null && this.state != ZeusRtmpSenderPlus.STATE.STOPPED && streamInfo != null) {
                if (!this.hasSpsPps()) {
                    return;
                }

                if (this.isClearQueue) {
                    if (!streamInfo.isVideoType) {
                        return;
                    }

                    if (streamInfo.videoPacketInfo == null) {
                        return;
                    }

                    if (!streamInfo.videoPacketInfo.isKeyframe) {
                        ++this.skipVideoFrameCount;
                        if (this.skipVideoFrameCount > 112) {
                            PushLog.e("cachePackageBuffer no keyframe!!!!!!!!!");
                            if (this.connectionListener != null) {
                                EventDispatcher.getInstance().post(new Runnable() {
                                    public void run() {
                                        RtmpHandler.this.skipVideoFrameCount = 0;
                                        RtmpHandler.this.connectionListener.onNoKeyframeError();
                                    }
                                });
                            }
                        }

                        return;
                    }
                }

                this.isClearQueue = false;
                this.skipVideoFrameCount = 0;
                this.processBandwidth(streamInfo);
                this.pendingDataSize += streamInfo.dataSize;
                this.streamBlockingQueue.offer(streamInfo);
            }

        }

        public synchronized void disConnectRtmp() {
            this.sendStop(false);
        }

        public synchronized void reconnectRtmp() {
            this.disConnectRtmp();
            if (this.configure != null && StringUtils.isNotEmpty(this.configure.rtmpAddr)) {
                this.sendStart(this.configure.rtmpAddr);
            }

        }

        private synchronized void clearSpsPps() {
            PushLog.e("=================== clearSpsPps =================");
            this.streamBlockingQueue.clear();
            this.isClearQueue = true;
            this.softVideoSequenceHeaderInfo = null;
            this.hardVideoSequenceHeaderInfo = null;
        }

        public synchronized boolean hasSpsPps() {
            return this.hardVideoSequenceHeaderInfo != null || this.softVideoSequenceHeaderInfo != null;
        }

        public synchronized void reSendRtmpFlvHeader(boolean softMode) {
            if (softMode) {
                if (this.softVideoSequenceHeaderInfo != null) {
                    this.sendVideoSequenceHeader(this.softVideoSequenceHeaderInfo, true);
                }
            } else if (this.hardVideoSequenceHeaderInfo != null) {
                this.sendVideoSequenceHeader(this.hardVideoSequenceHeaderInfo, false);
            }

            this.sendAudioSequenceHeader();
        }

        public void setConnectionListener(IConnectionListener connectionListener) {
            synchronized (this.syncConnectionListener) {
                this.connectionListener = connectionListener;
            }
        }

        public int getTotalSpeed() {
            return (this.getVideoSpeed() + this.getAudioSpeed()) * 8 / 1024;
        }

        private int getTotalSpeedByte() {
            return this.getVideoSpeed() + this.getAudioSpeed();
        }

        public int getVideoSpeed() {
            return this.videoByteSpeedometer.getSpeed();
        }

        public int getAudioSpeed() {
            return this.audioByteSpeedometer.getSpeed();
        }

        public float getVideoSendFps() {
            return this.videoSendFrameRateMeter.getFps();
        }

        public ZeusRtmpSenderPlus.StatisticsBandwidth getBandwidthStatisticsAndRestart() {
            ZeusRtmpSenderPlus.StatisticsBandwidth result = (ZeusRtmpSenderPlus.StatisticsBandwidth) this.statisticsBandwidth.clone();
            this.statisticsBandwidth.restart();
            return result;
        }

        public boolean isRtmpConnected() {
            return this.isRtmpConnected == 0;
        }

        public void destroy() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String url = (String) msg.obj;
                    if (this.state != ZeusRtmpSenderPlus.STATE.RUNNING && !StringUtils.isEmpty(url)) {
                        this.videoSendFrameRateMeter.reSet();

                        for (this.isRtmpConnected = this.rtmpConnect(url); !this.isRtmpConnected() && this.connRetryCount < 1; this.isRtmpConnected = this.rtmpConnect(url)) {
                            PushLog.d("retry connect rtmp...");
                            ++this.connRetryCount;
                        }

                        PushLog.d("connect rtmp end..." + this.isRtmpConnected);
                        final int res = this.isRtmpConnected;
                        synchronized (this.syncConnectionListener) {
                            if (this.connectionListener != null) {
                                EventDispatcher.getInstance().post(new Runnable() {
                                    public void run() {
                                        RtmpHandler.this.connectionListener.onOpenConnectionCallback(res);
                                    }
                                });
                            }
                        }

                        if (this.isRtmpConnected()) {
                            ZeusRtmpSenderPlus.this.resetState();
                            this.state = ZeusRtmpSenderPlus.STATE.RUNNING;
                            PushLog.e("RtmpSender connect success======>>>>>>");
                            if (this.configure.isSoftEncodeMode()) {
                                if (this.softVideoSequenceHeaderInfo != null) {
                                    PushLog.d("send soft encoder video sps after connect rtmp");
                                    this.sendVideoSequenceHeader(this.softVideoSequenceHeaderInfo, true);
                                }
                            } else if (this.hardVideoSequenceHeaderInfo != null) {
                                PushLog.e("send hard encoder video sps after connect rtmp");
                                this.sendVideoSequenceHeader(this.hardVideoSequenceHeaderInfo, false);
                            } else {
                                PushLog.e("hardVideoSequenceHeaderInfo is null");
                            }

                            this.sendAudioSequenceHeader();
                            if (this.configure != null && StringUtils.isNotEmpty(this.configure.rtmpAddr)) {
                                if (this.configure.isHardEncodeMode()) {
                                    RtmpClient.sendEncoderCodecType(false, (double) this.configure.videoWidth, (double) this.configure.videoHeight, (double) this.configure.videoFPS, (double) this.configure.videoBitRateMax);
                                } else if (this.configure.isSoftEncodeMode()) {
                                    RtmpClient.sendEncoderCodecType(true, (double) this.configure.videoWidth, (double) this.configure.videoHeight, (double) this.configure.videoFPS, (double) this.configure.videoBitRateMax);
                                }

                                PushLog.e("rtmp sendEncoderCodecType:" + this.configure.videoWidth + "," + this.configure.videoHeight + "," + this.configure.videoFPS + "," + this.configure.videoBitRateMax);
                            }

                            PushLog.d("start loop after connect rtmp");
                            this.removeMessages(7);
                            this.sendEmptyMessage(7);
                        }
                    }
                    break;
                case 2:
                    PushLog.e("rtmp handler stop===============>1");
                    if (this.state != ZeusRtmpSenderPlus.STATE.STOPPED) {
                        this.state = ZeusRtmpSenderPlus.STATE.STOPPED;
                        boolean isLiveEnd = (Boolean) msg.obj;
                        PushLog.e("rtmp handler stop===============>2 isLiveEnd=" + isLiveEnd);
                        RtmpClient.stop(isLiveEnd);
                        this.isRtmpConnected = -11;
                        synchronized (this.syncConnectionListener) {
                            if (this.connectionListener != null) {
                                EventDispatcher.getInstance().post(new Runnable() {
                                    public void run() {
                                        RtmpHandler.this.connectionListener.onCloseConnectionCallback(1);
                                    }
                                });
                            }
                        }

                        synchronized (this.syncConnectionListener) {
                            this.videoPackageErrorNum = 0;
                            this.audioPackageErrorNum = 0;
                        }

                        if (this.streamBlockingQueue != null) {
                            this.streamBlockingQueue.clear();
                            this.isClearQueue = true;
                            this.pendingDataSize = 0;
                        }
                    }
                    break;
                case 3:
                    if (this.state == ZeusRtmpSenderPlus.STATE.RUNNING) {
                        ZeusRtmpSenderPlus.VideoSequenceHeaderInfo videoSequenceHeaderInfo = (ZeusRtmpSenderPlus.VideoSequenceHeaderInfo) msg.obj;
                        RtmpClient.sendVideoSequenceHeader(videoSequenceHeaderInfo.spsData, videoSequenceHeaderInfo.spsSize, videoSequenceHeaderInfo.ppsData, videoSequenceHeaderInfo.ppsSize);
                        PushLog.e("rtmp sendVideoSequenceHeader");
                    }
                case 4:
                case 6:
                default:
                    break;
                case 5:
                    if (this.state == ZeusRtmpSenderPlus.STATE.RUNNING) {
                        PushLog.e("rtmp sendAudioSequenceHeader，" + Configure.audioConfig.getFixedSampleRates() + "，" + 1);
                        RtmpClient.sendAudioSequenceHeader(Configure.audioConfig.getFixedSampleRates(), 1);
                    }
                    break;
                case 7:
                    if (this.state == ZeusRtmpSenderPlus.STATE.RUNNING) {
                        if (this.streamBlockingQueue != null) {
                            ZeusRtmpSenderPlus.StreamInfo streamInfo = (ZeusRtmpSenderPlus.StreamInfo) this.streamBlockingQueue.poll();
                            if (streamInfo != null) {
                                this.delayMillis = 0L;
                                if (this.pendingDataSize > 0) {
                                    this.pendingDataSize -= streamInfo.dataSize;
                                }

                                boolean isVideoSuccess;
                                if (streamInfo.isVideoType) {
                                    ZeusRtmpSenderPlus.VideoPacketInfo videoPacketInfo = streamInfo.videoPacketInfo;
                                    isVideoSuccess = false;
                                    if (this.isAsusMobile) {
                                        isVideoSuccess = RtmpClient.sendAsusVideoPacket(videoPacketInfo.videoData, videoPacketInfo.videoSize, videoPacketInfo.timeoffset, videoPacketInfo.isKeyframe);
                                    } else {
                                        isVideoSuccess = RtmpClient.sendVideoPacket(videoPacketInfo.videoData, videoPacketInfo.videoSize, videoPacketInfo.timeoffset, videoPacketInfo.isKeyframe);
                                    }

                                    if (isVideoSuccess) {
                                        this.videoPackageErrorNum = 0;
                                        this.videoByteSpeedometer.gain(videoPacketInfo.videoSize);
                                        this.videoSendFrameRateMeter.count();
                                    } else {
                                        ++this.videoPackageErrorNum;
                                        synchronized (this.syncConnectionListener) {
                                            if (this.connectionListener != null) {
                                                EventDispatcher.getInstance().post(new IConnectionListener.IWriteErrorRunnable(this.connectionListener, 1, this.videoPackageErrorNum));
                                            }
                                        }
                                    }
                                } else {
                                    ZeusRtmpSenderPlus.AudioPacketInfo audioPacketInfo = streamInfo.audioPacketInfo;
                                    isVideoSuccess = RtmpClient.sendAudioPacket(audioPacketInfo.audioData, audioPacketInfo.audioSize, audioPacketInfo.timeoffset, audioPacketInfo.hasHeader);
                                    if (isVideoSuccess) {
                                        this.audioPackageErrorNum = 0;
                                        this.audioByteSpeedometer.gain(audioPacketInfo.audioSize);
                                    } else {
                                        ++this.audioPackageErrorNum;
                                        synchronized (this.syncConnectionListener) {
                                            if (this.connectionListener != null) {
                                                EventDispatcher.getInstance().post(new IConnectionListener.IWriteErrorRunnable(this.connectionListener, 2, this.audioPackageErrorNum));
                                            }
                                        }
                                    }
                                }
                            } else {
                                this.delayMillis = 2L;
                            }
                        }

                        if (this.state == ZeusRtmpSenderPlus.STATE.RUNNING && ZeusRtmpSenderPlus.this.rtmpHandler != null) {
                            this.sendEmptyMessageDelayed(7, this.delayMillis);
                        }
                    }
            }

        }

        private int rtmpConnect(String url) {
            PushLog.d("rtmp connect url=" + url);
            RtmpClient.stop(false);
            boolean isNetStreamReady = false;
            int sockType = this.configure.sockType;
            boolean isInit = RtmpClient.init(url, sockType);
            RtmpClient.setTimeout(5, 5);
            if (sockType == 1) {
                RtmpClient.parseQuicConfig(this.configure.quicConnIdleTimeout, this.configure.quicCryptoMaxTime, this.configure.quicCryptoIdleTime, this.configure.quicPingTimeout, this.configure.quicPingReduceTimeout, this.configure.quicChunkSize);
            }

            PushLog.e("rtmpsenderplus init:" + isInit + ",chunk size:" + this.configure.quicChunkSize);
            int result = -10;
            int isConnectRet = RtmpClient.connect();
            if (isConnectRet == 0) {
                isNetStreamReady = RtmpClient.createNetStream();
            } else {
                result = isConnectRet;
            }

            PushLog.e("rtmpsenderplus isConnect:" + isConnectRet + ",isNetStreamReady:" + isNetStreamReady);
            if (isInit && isConnectRet == 0 && isNetStreamReady) {
                result = 0;
            }

            return result;
        }
    }

    private static enum STATE {
        IDLE,
        RUNNING,
        STOPPED;

        private STATE() {
        }
    }
}
