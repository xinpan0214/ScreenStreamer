package com.xucz.libscreenstream.pusher.screenstream;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.xucz.libscreenstream.helper.MediaCodecHelper;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class ScreenDataThread extends Thread {
    private static final long WAIT_TIME = 10000L;
    private MediaCodec.BufferInfo eInfo = new MediaCodec.BufferInfo();
    private long startTime = 0L;
    private MediaCodec dstVideoEncoder;
    private ScreenEncoder.ScreenEncoderCallback dataCollecter;
    private final Object mLock = new Object();
    private boolean isRunning = true;
    private AtomicBoolean shouldQuit = new AtomicBoolean(false);
    private int encodeErrorNum = 0;
    private BaseMp4Muxer baseMp4Muxer;

    public ScreenDataThread(MediaCodec encoder, boolean isAsusMobile, ScreenEncoder.ScreenEncoderCallback callback, BaseMp4Muxer baseMp4Muxer) {
        super("ScreenDataThread");
        this.startTime = 0L;
        this.dstVideoEncoder = encoder;
        this.dataCollecter = callback;
        this.baseMp4Muxer = baseMp4Muxer;
    }

    void quit() {
        this.onResume();
        this.shouldQuit.set(true);
        this.interrupt();
    }

    void onResume() {
        if (!this.isRunning) {
            synchronized (this.mLock) {
                this.isRunning = true;
                this.mLock.notifyAll();
            }
        }
    }

    void onPause() {
        this.isRunning = false;
    }

    public void run() {
        for (; !this.shouldQuit.get() && this.dstVideoEncoder != null; this.deliverEncodedImage()) {
            synchronized (this.mLock) {
                if (!this.isRunning) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException var4) {
                        var4.printStackTrace();
                    }
                }
            }
        }

        PushLog.e("Hard VideoSendThread quit success------>");
    }

    private void deliverEncodedImage() {
        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int index = this.dstVideoEncoder.dequeueOutputBuffer(info, 10000L);
            if (index < 0) {
                if (index == -2) {
                    try {
                        MediaFormat mediaFormat = this.dstVideoEncoder.getOutputFormat();
                        byte[] spsData = this.getSPSByte(mediaFormat);
                        byte[] ppsData = this.getPPSByte(mediaFormat);
                        if (this.baseMp4Muxer != null) {
                            this.baseMp4Muxer.storeMediaFormat(10, mediaFormat, spsData, ppsData);
                        }

                        PushLog.i("VideoSenderThread Mediacodec -----------> sps len=" + spsData.length + ",pps len =" + ppsData.length);
                        this.dataCollecter.onVideoSequenceHeader(spsData, spsData.length, ppsData, ppsData.length, false);
                    } catch (Exception var7) {
                        var7.printStackTrace();
                    }
                }

                return;
            }

            ByteBuffer codecOutputBuffer = this.dstVideoEncoder.getOutputBuffers()[index];
            if ((info.flags & 2) != 0) {
                PushLog.d("Config frame generated. Offset: " + info.offset + ". Size: " + info.size);
            } else {
                boolean isKeyFrame = (info.flags & 1) != 0;
                if (isKeyFrame) {
                    PushLog.d("Sync frame generated");
                }

                if (this.startTime == 0L) {
                    this.startTime = info.presentationTimeUs / 1000L;
                }

                if (this.baseMp4Muxer != null) {
                    ByteBuffer dataForMp4 = codecOutputBuffer.duplicate();
                    MediaCodec.BufferInfo bufferInfo = MediaCodecHelper.cloneBufferInfo(info);
                    this.baseMp4Muxer.writeSampleData(10, dataForMp4, bufferInfo);
                }

                if (info.size > 0) {
                    codecOutputBuffer.position(info.offset + 4);
                    codecOutputBuffer.limit(info.offset + info.size);
                    long tms = info.presentationTimeUs / 1000L - this.startTime;
                    this.sendRealData(tms, codecOutputBuffer, isKeyFrame);
                }
            }

            this.encodeErrorNum = 0;
            this.dstVideoEncoder.releaseOutputBuffer(index, false);
        } catch (Exception var8) {
            var8.printStackTrace();
            ++this.encodeErrorNum;
            if (this.encodeErrorNum > 100) {
                this.dataCollecter.onVideoEncodeError(0);
            }
        }

    }

    private void sendRealData(long tms, ByteBuffer realData, boolean isKeyFrame) {
        int realDataLength = realData.remaining();
        if (realDataLength > 0) {
            byte[] finalBuff = new byte[realDataLength];
            realData.get(finalBuff, 0, realDataLength);
            int frameType = finalBuff[0] & 31;
            this.dataCollecter.onVideoPacket(finalBuff, realDataLength, (int) tms, isKeyFrame);
        }
    }

    private byte[] getSPSByte(MediaFormat mediaFormat) {
        ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
        SPSByteBuff.position(4);
        int spslength = SPSByteBuff.remaining();
        byte[] sps = new byte[spslength];
        SPSByteBuff.get(sps, 0, spslength);
        return sps;
    }

    private byte[] getPPSByte(MediaFormat mediaFormat) {
        ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
        PPSByteBuff.position(4);
        int ppslength = PPSByteBuff.remaining();
        byte[] pps = new byte[ppslength];
        PPSByteBuff.get(pps, 0, ppslength);
        return pps;
    }
}
