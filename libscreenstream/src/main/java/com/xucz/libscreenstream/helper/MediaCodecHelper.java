package com.xucz.libscreenstream.helper;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.xucz.libscreenstream.config.Configure;
import com.xucz.libscreenstream.log.PushLog;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class MediaCodecHelper {
    public MediaCodecHelper() {
    }

    @Nullable
    public static MediaCodec createAudioMediaCodec(MediaFormat audioFormat) {
        audioFormat.setString("mime", "audio/mp4a-latm");
        audioFormat.setInteger("aac-profile", 2);
        audioFormat.setInteger("sample-rate", Configure.audioConfig.getFixedSampleRates());
        audioFormat.setInteger("channel-count", 1);
        audioFormat.setInteger("bitrate", 65536);
        audioFormat.setInteger("max-input-size", 8192);
        PushLog.d("creatingAudioEncoder,format=" + audioFormat.toString());

        try {
            MediaCodec result = MediaCodec.createEncoderByType(audioFormat.getString("mime"));
            return result;
        } catch (Exception var3) {
            PushLog.e("can`t create audioEncoder! -- %s", var3.toString());
            return null;
        }
    }

    @TargetApi(18)
    @Nullable
    public static MediaCodec createHardVideoMediaCodec(Configure coreParameters, MediaFormat videoFormat, boolean isUseCQMode) {
        videoFormat.setString("mime", "video/avc");
        videoFormat.setInteger("width", coreParameters.videoWidth);
        videoFormat.setInteger("height", coreParameters.videoHeight);
        videoFormat.setInteger("color-format", 2130708361);
        videoFormat.setInteger("bitrate", coreParameters.videoBitRate);
        videoFormat.setInteger("frame-rate", coreParameters.videoFPS);
        videoFormat.setInteger("i-frame-interval", 2);
        if (Build.VERSION.SDK_INT >= 21) {
            boolean isCBRSupport;
            if (isUseCQMode) {
                isCBRSupport = isSupportBitrateMode(0);
                boolean isVBRSupport = isSupportBitrateMode(1);
                PushLog.e("yjt isCQSupport = " + isCBRSupport + ",isVBRSupport = " + isVBRSupport);
                if (isVBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 1);
                }
            } else {
                isCBRSupport = isSupportBitrateMode(2);
                PushLog.e("yjt isCBRSupport = " + isCBRSupport);
                if (isCBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 2);
                }
            }
        }

        PushLog.d("createHardVideoMediaCodec,format=" + videoFormat.toString() + ",isUseCQ:" + isUseCQMode);
        MediaCodec result = null;

        try {
            result = MediaCodec.createEncoderByType(videoFormat.getString("mime"));
            if (coreParameters.enableHighMode > 0) {
                setProfileLevel(coreParameters, result, videoFormat);
            }

            return result;
        } catch (Exception var5) {
            var5.printStackTrace();
            PushLog.e(var5.getMessage());
            return null;
        }
    }

    public static MediaFormat createMediaFormat(Configure coreParameters, boolean isUseCQMode) {
        MediaFormat videoFormat = new MediaFormat();
        videoFormat.setString("mime", "video/avc");
        videoFormat.setInteger("width", coreParameters.videoWidth);
        videoFormat.setInteger("height", coreParameters.videoHeight);
        videoFormat.setInteger("color-format", 2130708361);
        videoFormat.setInteger("bitrate", coreParameters.videoBitRate);
        videoFormat.setInteger("frame-rate", coreParameters.videoFPS);
        videoFormat.setInteger("i-frame-interval", 2);
        if (Build.VERSION.SDK_INT >= 21) {
            boolean isCBRSupport;
            if (isUseCQMode) {
                isCBRSupport = isSupportBitrateMode(0);
                boolean isVBRSupport = isSupportBitrateMode(1);
                PushLog.e("yjt isCQSupport = " + isCBRSupport + ",isVBRSupport = " + isVBRSupport);
                if (isVBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 1);
                }
            } else {
                isCBRSupport = isSupportBitrateMode(2);
                PushLog.e("yjt isCBRSupport = " + isCBRSupport);
                if (isCBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 2);
                }
            }
        }

        return videoFormat;
    }

    public static boolean isH264HighProfileSupported(MediaCodecInfo info) {
        return Build.VERSION.SDK_INT > 24 && info.getName().startsWith("OMX.Exynos.");
    }

    public static MediaFormat createVideoFormat(int width, int height, int bitrate, int framerate, int gopSize, boolean isUseVBRMode) {
        MediaFormat videoFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        videoFormat.setInteger("color-format", 2130708361);
        videoFormat.setInteger("bitrate", bitrate);
        videoFormat.setInteger("frame-rate", framerate);
        videoFormat.setInteger("i-frame-interval", gopSize / framerate);
        if (Build.VERSION.SDK_INT >= 21) {
            boolean isVBRSupport;
            if (isUseVBRMode) {
                isVBRSupport = isSupportBitrateMode(1);
                if (isVBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 1);
                }
            } else {
                isVBRSupport = isSupportBitrateMode(2);
                if (isVBRSupport) {
                    videoFormat.setInteger("bitrate-mode", 2);
                }
            }
        }

        return videoFormat;
    }

    public static void setProfileLevel(Configure configure, MediaCodec codec, MediaFormat format) {
        if (Build.VERSION.SDK_INT >= 21 && codec != null && format != null) {
            String mime = format.getString("mime");
            MediaCodecInfo.CodecProfileLevel[] profileLevels = codec.getCodecInfo().getCapabilitiesForType(mime).profileLevels;
            MediaCodecInfo.CodecProfileLevel selected = new MediaCodecInfo.CodecProfileLevel();
            selected.profile = -1;
            selected.level = -1;
            MediaCodecInfo.CodecProfileLevel[] var6 = profileLevels;
            int var7 = profileLevels.length;

            for (int var8 = 0; var8 < var7; ++var8) {
                MediaCodecInfo.CodecProfileLevel p = var6[var8];
                PushLog.e("profile item: " + p.profile + ", " + p.level);
                if (supportsProfileLevel(p.profile, p.level, profileLevels, mime) && 8 == p.profile && p.profile > selected.profile && Build.VERSION.SDK_INT >= 24) {
                    selected = p;
                }
            }

            PushLog.e("selected: " + selected.profile + ", " + selected.level + ", " + supportsProfileLevel(selected.profile, selected.level, profileLevels, mime));
            if (-1 != selected.profile) {
                setProfileLevel(configure, format, selected.profile, selected.level);
            }
        }
    }

    private static void setProfileLevel(Configure configure, MediaFormat format, int profile, int level) {
        if (configure != null) {
            configure.mediacodecAVCProfile = profile;
            configure.mediacodecAVClevel = level;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            format.setInteger("profile", profile);
            format.setInteger("level", level);
        }

    }

    public static boolean supportsProfileLevel(int profile, Integer level, MediaCodecInfo.CodecProfileLevel[] profileLevels, String mime) {
        MediaCodecInfo.CodecProfileLevel[] var4 = profileLevels;
        int var5 = profileLevels.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            MediaCodecInfo.CodecProfileLevel pl = var4[var6];
            if (pl.profile == profile) {
                if (level == null || mime.equalsIgnoreCase("audio/mp4a-latm")) {
                    return true;
                }

                if ((!mime.equalsIgnoreCase("video/3gpp") || pl.level == level || pl.level != 16 || level <= 1) && (!mime.equalsIgnoreCase("video/mp4v-es") || pl.level == level || pl.level != 4 || level <= 1) && Build.VERSION.SDK_INT >= 21) {
                    int HEVCHighTierLevels = 44739242;
                    if (mime.equalsIgnoreCase("video/hevc")) {
                        boolean supportsHighTier = (pl.level & 44739242) != 0;
                        boolean checkingHighTier = (level & 44739242) != 0;
                        if (checkingHighTier && !supportsHighTier) {
                            continue;
                        }
                    }

                    if (pl.level >= level) {
                        if (MediaCodecInfo.CodecCapabilities.createFromProfileLevel(mime, profile, pl.level) != null) {
                            return MediaCodecInfo.CodecCapabilities.createFromProfileLevel(mime, profile, level) != null;
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isArrayContain(int[] src, int target) {
        int[] var2 = src;
        int var3 = src.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            int color = var2[var4];
            if (color == target) {
                return true;
            }
        }

        return false;
    }

    private static boolean isProfileContain(MediaCodecInfo.CodecProfileLevel[] src, int target) {
        MediaCodecInfo.CodecProfileLevel[] var2 = src;
        int var3 = src.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            MediaCodecInfo.CodecProfileLevel color = var2[var4];
            if (color.profile == target) {
                return true;
            }
        }

        return false;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();

        for (int i = 0; i < numCodecs; ++i) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();

                for (int j = 0; j < types.length; ++j) {
                    if (types[j].equalsIgnoreCase(mimeType)) {
                        return codecInfo;
                    }
                }
            }
        }

        return null;
    }

    @RequiresApi(
            api = 21
    )
    public static MediaCodecInfo.VideoCapabilities getVideoCapabilities() {
        MediaCodecInfo mediaCodecInfo = selectCodec("video/avc");
        if (mediaCodecInfo == null) {
            return null;
        } else {
            MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType("video/avc");
            if (codecCapabilities == null) {
                return null;
            } else {
                MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                return videoCapabilities;
            }
        }
    }

    @TargetApi(21)
    public static boolean isSupportBitrateMode(int mode) {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        } else {
            try {
                MediaCodecInfo mediaCodecInfo = selectCodec("video/avc");
                if (mediaCodecInfo != null) {
                    MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType("video/avc");
                    if (codecCapabilities != null) {
                        MediaCodecInfo.EncoderCapabilities encoderCapabilities = codecCapabilities.getEncoderCapabilities();
                        if (encoderCapabilities != null) {
                            return encoderCapabilities.isBitrateModeSupported(mode);
                        }
                    }
                }
            } catch (Exception var4) {
                var4.printStackTrace();
            }

            return false;
        }
    }

    @TargetApi(21)
    public static boolean isSizeSupported(int width, int height) {
        if (Build.VERSION.SDK_INT < 21) {
            return true;
        } else {
            MediaCodecInfo.VideoCapabilities videoCapabilities = getVideoCapabilities();
            return videoCapabilities != null ? videoCapabilities.isSizeSupported(width, height) : false;
        }
    }

    public static MediaCodec.BufferInfo cloneBufferInfo(MediaCodec.BufferInfo src) {
        if (src == null) {
            return null;
        } else {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.size = src.size;
            bufferInfo.presentationTimeUs = src.presentationTimeUs;
            bufferInfo.flags = src.flags;
            bufferInfo.offset = src.offset;
            return bufferInfo;
        }
    }
}
