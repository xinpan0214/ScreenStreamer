package com.xucz.libscreenstream.pusher.packager;

import android.media.MediaFormat;

import com.xucz.libscreenstream.tools.ByteArrayTools;

import java.nio.ByteBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class Packager {
    public Packager() {
    }

    public static class FLVPackager {
        public static final int FLV_TAG_LENGTH = 11;
        public static final int FLV_VIDEO_TAG_LENGTH = 5;
        public static final int FLV_AUDIO_TAG_LENGTH = 2;
        public static final int FLV_TAG_FOOTER_LENGTH = 4;
        public static final int NALU_HEADER_LENGTH = 4;

        public FLVPackager() {
        }

        public static void fillFlvVideoTag(byte[] dst, int pos, boolean isAVCSequenceHeader, boolean isIDR, int readDataLength) {
            dst[pos] = (byte) (isIDR ? 23 : 39);
            dst[pos + 1] = (byte) (isAVCSequenceHeader ? 0 : 1);
            dst[pos + 2] = 0;
            dst[pos + 3] = 0;
            dst[pos + 4] = 0;
            if (!isAVCSequenceHeader) {
                ByteArrayTools.intToByteArrayFull(dst, pos + 5, readDataLength);
            }

        }

        public static void fillFlvAudioTag(byte[] dst, int pos, boolean isAACSequenceHeader) {
            dst[pos] = -82;
            dst[pos + 1] = (byte) (isAACSequenceHeader ? 0 : 1);
        }
    }

    public static class H264Packager {
        public H264Packager() {
        }

        public static byte[] generateAVCDecoderConfigurationRecord(MediaFormat mediaFormat) {
            ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
            SPSByteBuff.position(4);
            ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
            PPSByteBuff.position(4);
            int spslength = SPSByteBuff.remaining();
            int ppslength = PPSByteBuff.remaining();
            int length = 11 + spslength + ppslength;
            byte[] result = new byte[length];
            SPSByteBuff.get(result, 8, spslength);
            PPSByteBuff.get(result, 8 + spslength + 3, ppslength);
            result[0] = 1;
            result[1] = result[9];
            result[2] = result[10];
            result[3] = result[11];
            result[4] = -1;
            result[5] = -31;
            ByteArrayTools.intToByteArrayTwoByte(result, 6, spslength);
            int pos = 8 + spslength;
            result[pos] = 1;
            ByteArrayTools.intToByteArrayTwoByte(result, pos + 1, ppslength);
            return result;
        }
    }
}
