package com.xucz.libscreenstream.config;

import android.content.res.Resources;
import android.media.AudioRecord;
import android.text.TextUtils;

import com.xucz.libscreenstream.entity.AnimationMap;
import com.xucz.libscreenstream.entity.VideoSize;
import com.xucz.libscreenstream.utils.DeviceUtils;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class Configure {
    public int sockType = 0;
    public int quicConnIdleTimeout = 60;
    public int quicCryptoMaxTime = 20;
    public int quicCryptoIdleTime = 20;
    public int quicPingTimeout = 5;
    public int quicPingReduceTimeout = 5;
    public int quicChunkSize = 1200;
    public Resources resources;
    public int filterMode;
    public VideoSize targetVideoSize;
    public int videoBufferQueueNum;
    public String rtmpAddr;
    public int renderingMode;
    private int defaultCamera = 1;
    public int frontCameraDirectionMode;
    public int backCameraDirectionMode;
    private boolean printDetailMsg;
    public boolean isPortrait;
    public int currentCameraIndex = -1;
    public int previewVideoWidth = 540;
    public int previewVideoHeight = 960;
    public int previewColorFormat = 17;
    public int videoWidth;
    public int videoHeight;
    public int videoBitRate = 600000;
    public int videoBitRateMax = 800000;
    public int videoBitRateMin = 400000;
    public int videoFPS = 18;
    public int previewMaxFps;
    public int previewMinFps;
    public int screenCaptureMaxFps;
    public boolean enableClear = false;
    public int senderQueueLength = 150;
    public double emptyBufferTime = 0.2D;
    public double overflowBufferTime = 6.0D;
    public double trashingBufferTime = 2.0D;
    public int trashingCountThreshold = 2;
    public int smoothingCountThreshold = 5;
    public int soft_min_fps = 10;
    public int soft_enable_server = 1;
    public String softPreset = "veryfast";
    public String softProfile = "high";
    public boolean hasDetectSoftEncode = false;
    public boolean enableSoftEncode = false;
    public int cameraRecordHint = 0;
    public float cropRatio = 0.0F;
    public int mediacodecAVCProfile;
    public int mediacodecAVClevel;
    public int openGLVersion;
    public static Configure.AudioConfig audioConfig = new Configure.AudioConfig() {
        public int getFixedSampleRates() {
            int[] var1 = new int[]{48000, 44100};
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                int rate = var1[var3];
                int bufferSize = AudioRecord.getMinBufferSize(rate, 16, 2);
                if (bufferSize > 0) {
                    return rate;
                }
            }

            return 44100;
        }
    };
    public int enableHighMode = 0;
    public AnimationMap animationMap;
    public boolean enableMultithreadingRender = false;
    private static String[] BadKeyframeMobiles = new String[]{"samsungSM-G355H", "ADVANS4F"};

    public Configure() {
    }

    public static int getAudioBufferSize() {
        int framesPerBuffer = audioConfig.getFixedSampleRates() / 100;
        return 2 * framesPerBuffer;
    }

    public void check() {
        if (this.openGLVersion <= 0) {
            throw new RuntimeException("Please set the openGLVersion（openGLVersion is use for PBO, use GLHelper.glVersion(context) to set this value）");
        } else if (null == this.resources) {
            throw new RuntimeException("Please set the resources（resources is use for AnimationFilter）");
        }
    }

    public int getBackCameraDirectionMode() {
        return this.backCameraDirectionMode;
    }

    public void setBackCameraDirectionMode(int backCameraDirectionMode) {
        this.backCameraDirectionMode = backCameraDirectionMode;
    }

    public int getDefaultCamera() {
        return this.defaultCamera;
    }

    public void setDefaultCamera(int defaultCamera) {
        this.defaultCamera = defaultCamera;
    }

    public int getFilterMode() {
        return this.filterMode;
    }

    public void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
    }

    public int getFrontCameraDirectionMode() {
        return this.frontCameraDirectionMode;
    }

    public void setFrontCameraDirectionMode(int frontCameraDirectionMode) {
        this.frontCameraDirectionMode = frontCameraDirectionMode;
    }

    public boolean isPrintDetailMsg() {
        return this.printDetailMsg;
    }

    public void setPrintDetailMsg(boolean printDetailMsg) {
        this.printDetailMsg = printDetailMsg;
    }

    public int getRenderingMode() {
        return this.renderingMode;
    }

    public void setRenderingMode(int renderingMode) {
        this.renderingMode = renderingMode;
    }

    public String getRtmpAddr() {
        return this.rtmpAddr;
    }

    public void setRtmpAddr(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
    }

    public VideoSize getTargetVideoSize() {
        return this.targetVideoSize;
    }

    public void setTargetVideoSize(VideoSize targetVideoSize) {
        this.targetVideoSize = targetVideoSize;
    }

    public int getVideoBufferQueueNum() {
        return this.videoBufferQueueNum;
    }

    public void setVideoBufferQueueNum(int videoBufferQueueNum) {
        this.videoBufferQueueNum = videoBufferQueueNum;
    }

    public boolean isHardEncodeMode() {
        return !TextUtils.isEmpty(this.rtmpAddr) && this.rtmpAddr.contains("hard");
    }

    public boolean isSoftEncodeMode() {
        return !TextUtils.isEmpty(this.rtmpAddr) && this.rtmpAddr.contains("soft");
    }

    public static boolean isAsusMobile() {
        String brand = DeviceUtils.getBuildBrand();
        return !TextUtils.isEmpty(brand) && "asus".equalsIgnoreCase(brand);
    }

    public static boolean isBadKeyframeMobile() {
        String deviceModel = DeviceUtils.getMobileModel();
        boolean flag = false;
        if (!TextUtils.isEmpty(deviceModel)) {
            for (int i = 0; i < BadKeyframeMobiles.length; ++i) {
                if (BadKeyframeMobiles[i].equalsIgnoreCase(deviceModel)) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    public interface SockConfig {
        int SOCK_TYPE_TCP = 0;
        int SOCK_TYPE_QUIC = 1;
        int DEFAULT_QUIC_CONN_IDLE_TIMEOUT = 60;
        int DEFAULT_QUIC_CRYPTO_MAX_TIME = 20;
        int DEFAULT_QUIC_CRYPTO_IDLE_TIME = 20;
        int DEFAULT_QUIC_PING_TIMEOUT = 5;
        int DEFAULT_QUIC_PING_REDUCE_TIMEOUT = 5;
        int DEFAULT_QUIC_CHUNK_SIZE = 1200;
    }

    public static class DirectionMode {
        public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = 1;
        public static final int FLAG_DIRECTION_FLIP_VERTICAL = 2;
        public static final int FLAG_DIRECTION_ROATATION_0 = 16;
        public static final int FLAG_DIRECTION_ROATATION_90 = 32;
        public static final int FLAG_DIRECTION_ROATATION_180 = 64;
        public static final int FLAG_DIRECTION_ROATATION_270 = 128;

        public DirectionMode() {
        }
    }

    public interface RenderingMode {
        int RENDERING_MODE_NATIVE_WINDOW = 1;
        int RENDERING_MODE_OPENGLES = 2;
    }

    public interface FilterMode {
        int FILTER_MODE_HARD = 1;
        int FILTER_MODE_SOFT = 2;
    }

    public interface VideoConfig {
        String MIME_TYPE = "video/avc";
        int FRAME_RATE = 18;
        int IFRAME_INTERVAL = 2;
        int BIT_RATE = 600000;
        int BIT_RATE_MAX = 800000;
        int BIT_RATE_MIN = 400000;
        int CAMERA_PREVIEW_FORMAT = 17;
        int PREVIEW_WIDTH = 540;
        int PREVIEW_HEIGHT = 960;
        double EMPTY_BUFFER_TIME = 0.2D;
        int SMOOTHING_COUNT_THRESHOLD = 5;
        double OVERFLOW_BURRER_TIME = 6.0D;
        double TRASHING_BUFFER_TIME = 2.0D;
        int TRASHING_COUNT_THRESHOLD = 2;
        int SOFT_INIT_BITRATE = 600000;
        int SOFT_MIN_BITRATE = 600000;
        int SOFT_MAX_BITRATE = 800000;
        int SOFT_MIN_FPS = 10;
        int SOFT_ENABLE = 1;
    }

    public interface AudioConfig {
        String MINE_TYPE = "audio/mp4a-latm";
        int SAMPLE_RATE_44100 = 44100;
        int SAMPLE_RATE_48000 = 48000;
        int CHANNEL_COUNT = 1;
        int BIT_RATE = 65536;
        int MAX_INPUT_SIZE = 8192;
        int AUDIO_RECODER_SLICE_SIZE = Configure.audioConfig.getFixedSampleRates() / 10;
        int AUDIO_RECODER_BUFFER_SIZE = AUDIO_RECODER_SLICE_SIZE * 2;
        int AUDIO_BUFFER_QUEUE_NUM = 15;
        int BITS_PER_SAMPLE = 16;
        int CALLBACK_BUFFER_SIZE_MS = 10;
        int BUFFERS_PER_SECOND = 100;
        int BUFFER_SIZE_FACTOR = 2;

        int getFixedSampleRates();
    }
}
