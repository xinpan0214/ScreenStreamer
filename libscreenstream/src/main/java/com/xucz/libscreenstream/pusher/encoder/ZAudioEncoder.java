package com.xucz.libscreenstream.pusher.encoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.entity.AudioBuffInfo;
import com.xucz.libscreenstream.helper.MediaCodecHelper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.pusher.packager.Packager;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;
import com.xucz.libscreenstream.rtmp.FlvData;
import com.xucz.libscreenstream.rtmp.IFlvDataCollect;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class ZAudioEncoder {
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_BUFFER_OVERFLOW = 11;
    public static final int ERROR_PTS = 12;
    private MediaCodec dstAudioEncoder;
    private MediaFormat dstAudioFormat;
    private AudioBuffInfo[] orignAudioBuffs;
    private int lastAudioQueueBuffIndex;
    private AudioBuffInfo orignAudioBuff;
    private ZAudioEncoder.AudioFilterHandler audioFilterHandler;
    private HandlerThread audioFilterHandlerThread;
    private ZAudioEncoder.AudioSenderThread audioSenderThread;
    private ZAudioEncoder.OnAudioListener onAudioListener;

    public ZAudioEncoder() {
    }

    public void setOnAudioListener(ZAudioEncoder.OnAudioListener onAudioListener) {
        this.onAudioListener = onAudioListener;
    }

    public void queueAudio(byte[] rawAudioFrame) {
        int targetIndex = (this.lastAudioQueueBuffIndex + 1) % this.orignAudioBuffs.length;
        if (this.orignAudioBuffs[targetIndex].isReadyToFill && this.audioFilterHandler != null) {
            System.arraycopy(rawAudioFrame, 0, this.orignAudioBuffs[targetIndex].buff, 0, rawAudioFrame.length);
            this.orignAudioBuffs[targetIndex].isReadyToFill = false;
            this.lastAudioQueueBuffIndex = targetIndex;
            this.audioFilterHandler.sendMessage(this.audioFilterHandler.obtainMessage(1, targetIndex, 0));
        } else {
            PushLog.d("queueAudio,abandon,targetIndex" + targetIndex);
        }

    }

    public boolean prepare() {
        this.dstAudioFormat = new MediaFormat();
        this.dstAudioEncoder = MediaCodecHelper.createAudioMediaCodec(this.dstAudioFormat);
        if (this.dstAudioEncoder == null) {
            PushLog.e("create Audio MediaCodec failed");
            return false;
        } else {
            int audioQueueNum = 15;
            int orignAudioBuffSize = Configure.getAudioBufferSize();
            this.orignAudioBuffs = new AudioBuffInfo[audioQueueNum];

            for (int i = 0; i < audioQueueNum; ++i) {
                this.orignAudioBuffs[i] = new AudioBuffInfo(2, orignAudioBuffSize);
            }

            this.orignAudioBuff = new AudioBuffInfo(2, orignAudioBuffSize);
            return true;
        }
    }

    public void start(IFlvDataCollect flvDataCollecter, BaseMp4Muxer baseMp4Muxer) {
        try {
            AudioBuffInfo[] var3 = this.orignAudioBuffs;
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                AudioBuffInfo buff = var3[var5];
                buff.isReadyToFill = true;
            }

            if (this.dstAudioEncoder == null) {
                this.dstAudioEncoder = MediaCodec.createEncoderByType(this.dstAudioFormat.getString("mime"));
            }

            this.dstAudioEncoder.configure(this.dstAudioFormat, (Surface) null, (MediaCrypto) null, 1);
            this.dstAudioEncoder.start();
            this.lastAudioQueueBuffIndex = 0;
            this.audioFilterHandlerThread = new HandlerThread("HandlerThread-AudioFilter");
            this.audioSenderThread = new ZAudioEncoder.AudioSenderThread("AudioSenderThread", this.dstAudioEncoder, flvDataCollecter, baseMp4Muxer);
            this.audioFilterHandlerThread.start();
            this.audioSenderThread.start();
            this.audioFilterHandler = new ZAudioEncoder.AudioFilterHandler(this.audioFilterHandlerThread.getLooper());
        } catch (Exception var7) {
            PushLog.e("RESSoftAudioCore", var7.toString());
        }

    }

    public void stop() {
        if (this.audioFilterHandler != null && this.audioFilterHandlerThread != null && this.audioSenderThread != null) {
            this.audioFilterHandler.removeCallbacksAndMessages((Object) null);
            if (this.audioFilterHandlerThread != null) {
                if (Build.VERSION.SDK_INT >= 18) {
                    this.audioFilterHandlerThread.quitSafely();
                } else {
                    this.audioFilterHandlerThread.quit();
                }

                try {
                    this.audioFilterHandlerThread.join(100L);
                } catch (InterruptedException var4) {
                    var4.printStackTrace();
                    PushLog.e("RESSoftAudioCore", var4.toString());
                }
            }

            if (this.audioSenderThread != null) {
                this.audioSenderThread.quit();

                try {
                    this.audioSenderThread.join(100L);
                } catch (InterruptedException var3) {
                    var3.printStackTrace();
                    PushLog.e("RESSoftAudioCore", var3.toString());
                }
            }
        }

        if (this.dstAudioEncoder != null) {
            try {
                this.dstAudioEncoder.stop();
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            this.dstAudioEncoder.release();
            this.dstAudioEncoder = null;
        }

    }

    public void destroy() {
        this.audioFilterHandler = null;
        this.audioFilterHandlerThread = null;
        this.audioSenderThread = null;
    }

    public interface OnAudioListener {
        void onAudioError(int var1);
    }

    class AudioSenderThread extends Thread {
        private static final long WAIT_TIME = 5000L;
        private MediaCodec.BufferInfo eInfo = new MediaCodec.BufferInfo();
        private long startTime = 0L;
        private MediaCodec dstAudioEncoder;
        private IFlvDataCollect dataCollecter;
        private BaseMp4Muxer baseMp4Muxer;
        private boolean shouldQuit = false;
        private long mLastTimeUs;
        private long frameCount;
        private boolean useStrictPts = false;

        AudioSenderThread(String name, MediaCodec encoder, IFlvDataCollect flvDataCollecter, BaseMp4Muxer baseMp4Muxer) {
            super(name);
            this.startTime = 0L;
            this.frameCount = 0L;
            this.useStrictPts = false;
            this.dstAudioEncoder = encoder;
            this.dataCollecter = flvDataCollecter;
            this.baseMp4Muxer = baseMp4Muxer;
        }

        void quit() {
            this.shouldQuit = true;
            this.interrupt();
        }

        public void run() {
            if (this.dstAudioEncoder != null && this.eInfo != null && this.dataCollecter != null) {
                while (!this.shouldQuit) {
                    int eobIndex = -1;

                    try {
                        eobIndex = this.dstAudioEncoder.dequeueOutputBuffer(this.eInfo, 5000L);
                    } catch (Exception var11) {
                        var11.printStackTrace();
                    }

                    switch (eobIndex) {
                        case -3:
                        case -1:
                            break;
                        case -2:
                            MediaFormat outputFormat = this.dstAudioEncoder.getOutputFormat();
                            if (this.baseMp4Muxer != null) {
                                this.baseMp4Muxer.storeMediaFormat(11, outputFormat, (byte[]) null, (byte[]) null);
                            }

                            this.dataCollecter.onAudioSequenceHeader();
                            break;
                        default:
                            try {
                                ByteBuffer realData = this.dstAudioEncoder.getOutputBuffers()[eobIndex];
                                if (this.eInfo.flags != 2 && this.eInfo.size != 0) {
                                    long currTime = System.nanoTime() / 1000L;
                                    if (this.startTime == 0L) {
                                        this.startTime = currTime;
                                    }

                                    long tms = currTime - this.startTime;
                                    if (this.baseMp4Muxer != null) {
                                        ByteBuffer duplicateByteBufferForMp4 = realData.duplicate();
                                        MediaCodec.BufferInfo bufferInfo = MediaCodecHelper.cloneBufferInfo(this.eInfo);
                                        bufferInfo.presentationTimeUs = tms;
                                        this.baseMp4Muxer.writeSampleData(11, duplicateByteBufferForMp4, bufferInfo);
                                    }

                                    realData.position(this.eInfo.offset);
                                    realData.limit(this.eInfo.offset + this.eInfo.size);
                                    this.sendRealData(this.getPts(tms) / 1000L, realData);
                                }

                                this.dstAudioEncoder.releaseOutputBuffer(eobIndex, false);
                            } catch (Exception var10) {
                                var10.printStackTrace();
                            }
                    }
                }

                this.eInfo = null;
            }
        }

        private long getPts(long tms) {
            if (!this.useStrictPts && this.eInfo.presentationTimeUs - this.mLastTimeUs < 0L) {
                this.useStrictPts = true;
                PushLog.e("Calculate pts by sample rate");
                if (null != ZAudioEncoder.this.onAudioListener) {
                    ZAudioEncoder.this.onAudioListener.onAudioError(12);
                }
            }

            this.mLastTimeUs = this.eInfo.presentationTimeUs;
            float pts = (float) this.frameCount * (1.0E9F / (float) Configure.audioConfig.getFixedSampleRates());
            ++this.frameCount;
            return this.useStrictPts ? (long) pts : tms;
        }

        private void sendAudioSpecificConfig(long tms, ByteBuffer realData) {
            int packetLen = 2 + realData.remaining();
            byte[] finalBuff = new byte[packetLen];
            realData.get(finalBuff, 2, realData.remaining());
            this.dataCollecter.onAudioPacket(finalBuff, packetLen, (int) tms, false);
            Packager.FLVPackager.fillFlvAudioTag(finalBuff, 0, true);
            FlvData resFlvData = new FlvData();
            resFlvData.byteBuffer = finalBuff;
            resFlvData.size = finalBuff.length;
            resFlvData.dts = (int) tms;
            resFlvData.flvTagType = 8;
        }

        private void sendRealData(long tms, ByteBuffer realData) {
            int length = realData.remaining();
            byte[] rawBuff = new byte[length];
            realData.get(rawBuff, 0, length);
            this.dataCollecter.onAudioPacket(rawBuff, length, (int) tms, false);
        }
    }

    private class AudioFilterHandler extends Handler {
        public static final int WHAT_INCOMING_BUFF = 1;

        AudioFilterHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int targetIndex = msg.arg1;
                long nowTimeMs = System.currentTimeMillis();
                System.arraycopy(ZAudioEncoder.this.orignAudioBuffs[targetIndex].buff, 0, ZAudioEncoder.this.orignAudioBuff.buff, 0, ZAudioEncoder.this.orignAudioBuff.buff.length);
                ZAudioEncoder.this.orignAudioBuffs[targetIndex].isReadyToFill = true;
                int eibIndex = 0;

                try {
                    eibIndex = ZAudioEncoder.this.dstAudioEncoder.dequeueInputBuffer(-1L);
                } catch (Exception var7) {
                    var7.printStackTrace();
                }

                if (eibIndex >= 0) {
                    try {
                        ByteBuffer dstAudioEncoderIBuffer = ZAudioEncoder.this.dstAudioEncoder.getInputBuffers()[eibIndex];
                        dstAudioEncoderIBuffer.position(0);
                        dstAudioEncoderIBuffer.put(ZAudioEncoder.this.orignAudioBuff.buff, 0, ZAudioEncoder.this.orignAudioBuff.buff.length);
                        ZAudioEncoder.this.dstAudioEncoder.queueInputBuffer(eibIndex, 0, ZAudioEncoder.this.orignAudioBuff.buff.length, nowTimeMs * 1000L, 0);
                    } catch (BufferOverflowException var8) {
                        if (null != ZAudioEncoder.this.onAudioListener) {
                            ZAudioEncoder.this.onAudioListener.onAudioError(11);
                        }

                        var8.printStackTrace();
                    } catch (Exception var9) {
                        var9.printStackTrace();
                    }
                } else {
                    PushLog.d("dstAudioEncoder.dequeueInputBuffer(-1)<0");
                }

            }
        }
    }
}
