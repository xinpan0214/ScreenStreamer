package com.xucz.libscreenstream.pusher.controller;

import android.media.AudioRecord;

import com.xucz.libscreenstream.callback.IPrepareListener;
import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.log.PushLog;
import com.xucz.libscreenstream.pusher.encoder.ZAudioEncoder;
import com.xucz.libscreenstream.recorder.BaseMp4Muxer;
import com.xucz.libscreenstream.rtmp.IFlvDataCollect;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class AudioPushController extends AbsPushController implements ZAudioEncoder.OnAudioListener {
    private final Object syncOp = new Object();
    private AudioPushController.AudioRecordThread audioRecordThread;
    private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private byte[] muteBuffer;
    private ZAudioEncoder softAudioCore;
    private boolean isDisableAudio = false;
    private BaseMp4Muxer baseMp4Muxer;
    private AudioPushController.AudioCallback audioCallback;

    public AudioPushController() {
    }

    public AudioPushController(BaseMp4Muxer baseMp4Muxer) {
        this.baseMp4Muxer = baseMp4Muxer;
    }

    public boolean prepare(IPrepareListener prepareListener) {
        synchronized (this.syncOp) {
            this.softAudioCore = new ZAudioEncoder();
            this.softAudioCore.setOnAudioListener(this);
            if (!this.softAudioCore.prepare()) {
                PushLog.e("AudioPushController,prepare failed");
                return false;
            } else {
                return this.prepareAudio();
            }
        }
    }

    public void start(IFlvDataCollect flvDataCollecter) {
        synchronized (this.syncOp) {
            this.softAudioCore.start(flvDataCollecter, this.baseMp4Muxer);
            this.audioRecord.startRecording();
            this.audioRecordThread = new AudioPushController.AudioRecordThread();
            this.audioRecordThread.start();
            PushLog.d("AudioPushController,start()");
        }
    }

    public void stop() {
        synchronized (this.syncOp) {
            if (this.audioRecordThread != null) {
                this.audioRecordThread.quit();

                try {
                    this.audioRecordThread.join(100L);
                } catch (Exception var5) {
                    var5.printStackTrace();
                }
            }

            if (this.softAudioCore != null) {
                this.softAudioCore.stop();
            }

            this.audioRecordThread = null;

            try {
                this.audioRecord.stop();
            } catch (Exception var4) {
                var4.printStackTrace();
            }

        }
    }

    public void release() {
        synchronized (this.syncOp) {
            if (this.audioRecord != null) {
                try {
                    this.audioRecord.release();
                } catch (Exception var4) {
                    var4.printStackTrace();
                }

                this.audioRecord = null;
            }

            if (this.softAudioCore != null) {
                this.softAudioCore.destroy();
                this.softAudioCore = null;
            }

        }
    }

    public void toggleAudio(boolean isDisable) {
        synchronized (this.syncOp) {
            this.isDisableAudio = isDisable;
        }
    }

    public boolean isDisableAudio() {
        return this.isDisableAudio;
    }

    private boolean prepareAudio() {
        int bufferSize = Configure.getAudioBufferSize();
        this.audioBuffer = new byte[bufferSize];
        this.muteBuffer = new byte[bufferSize];
        int minBufferSize = AudioRecord.getMinBufferSize(Configure.audioConfig.getFixedSampleRates(), 16, 2);
        int bufferSizeInBytes = Math.max(2 * minBufferSize, this.audioBuffer.length);
        PushLog.e("AudioPushController bufferSizeInBytes = " + bufferSizeInBytes);

        try {
            this.audioRecord = new AudioRecord(1, Configure.audioConfig.getFixedSampleRates(), 16, 2, bufferSizeInBytes);
        } catch (IllegalArgumentException var5) {
            var5.printStackTrace();
            return false;
        }

        if (1 != this.audioRecord.getState()) {
            PushLog.e("AudioPushController audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        } else {
            return true;
        }
    }

    public void onAudioError(int error) {
        if (this.audioCallback != null) {
            this.audioCallback.onAudioRecordError(error);
        }

    }

    public void setAudioCallback(AudioPushController.AudioCallback audioCallback) {
        this.audioCallback = audioCallback;
    }

    public interface AudioCallback {
        void onAudioRecordError(int var1);
    }

    private class AudioRecordThread extends Thread {
        private boolean isRunning = true;
        private int errorCount;

        AudioRecordThread() {
            super("Thread-AudioRecord");
            this.isRunning = true;
        }

        public void quit() {
            this.isRunning = false;
        }

        public void run() {
            PushLog.d("AudioRecordThread,tid=" + Thread.currentThread().getId());

            while (this.isRunning && AudioPushController.this.audioRecord != null) {
                int size = AudioPushController.this.audioRecord.read(AudioPushController.this.audioBuffer, 0, AudioPushController.this.audioBuffer.length);
                if (this.isRunning && AudioPushController.this.softAudioCore != null) {
                    if (AudioPushController.this.isDisableAudio) {
                        AudioPushController.this.softAudioCore.queueAudio(AudioPushController.this.muteBuffer);
                    } else if (size > 0) {
                        AudioPushController.this.softAudioCore.queueAudio(AudioPushController.this.audioBuffer);
                    } else {
                        ++this.errorCount;
                        if (this.errorCount > 10000) {
                            this.errorCount = 1001;
                        }

                        if (this.errorCount == 220 && AudioPushController.this.audioCallback != null) {
                            AudioPushController.this.audioCallback.onAudioRecordError(0);
                        }
                    }
                }
            }

        }
    }
}
