package com.xucz.libscreenstream.pusher.packager;

import java.nio.ByteBuffer;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class FlvPackerHelper {
    public static final int FLV_HEAD_SIZE = 9;
    public static final int VIDEO_HEADER_SIZE = 5;
    public static final int AUDIO_HEADER_SIZE = 2;
    public static final int FLV_TAG_HEADER_SIZE = 11;
    public static final int PRE_SIZE = 4;
    public static final int AUDIO_SPECIFIC_CONFIG_SIZE = 2;
    public static final int VIDEO_SPECIFIC_CONFIG_EXTEND_SIZE = 11;

    public FlvPackerHelper() {
    }

    public static void writeFlvHeader(ByteBuffer buffer, boolean hasVideo, boolean hasAudio) {
        byte[] signature = new byte[]{70, 76, 86};
        byte version = 1;
        int videoFlag = hasVideo ? 1 : 0;
        int audioFlag = hasAudio ? 4 : 0;
        byte flags = (byte) (videoFlag | audioFlag);
        byte[] offset = new byte[]{0, 0, 0, 9};
        buffer.put(signature);
        buffer.put(version);
        buffer.put(flags);
        buffer.put(offset);
    }

    public static void writeFlvTagHeader(ByteBuffer buffer, int type, int dataSize, int timestamp) {
        int sizeAndType = dataSize & 16777215 | (type & 31) << 24;
        buffer.putInt(sizeAndType);
        int time = timestamp << 8 & -256 | timestamp >> 24 & 255;
        buffer.putInt(time);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
    }

    public static void writeFirstVideoTag(ByteBuffer buffer, byte[] sps, byte[] pps) {
        writeVideoHeader(buffer, 1, 7, 0);
        buffer.put((byte) 1);
        buffer.put(sps[1]);
        buffer.put(sps[2]);
        buffer.put(sps[3]);
        buffer.put((byte) -1);
        buffer.put((byte) -31);
        buffer.putShort((short) sps.length);
        buffer.put(sps);
        buffer.put((byte) 1);
        buffer.putShort((short) pps.length);
        buffer.put(pps);
    }

    public static void writeVideoHeader(ByteBuffer buffer, int flvVideoFrameType, int codecID, int AVCPacketType) {
        byte first = (byte) ((flvVideoFrameType & 15) << 4 | codecID & 15);
        buffer.put(first);
        buffer.put((byte) AVCPacketType);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
    }

    public static void writeH264Packet(ByteBuffer buffer, byte[] data, boolean isKeyFrame) {
        int flvVideoFrameType = 2;
        if (isKeyFrame) {
            flvVideoFrameType = 1;
        }

        writeVideoHeader(buffer, flvVideoFrameType, 7, 1);
        buffer.put(data);
    }

    public static void writeFirstAudioTag(ByteBuffer buffer, int audioRate, boolean isStereo, int audioSize) {
        byte[] audioInfo = new byte[2];
        int soundRateIndex = getAudioSimpleRateIndex(audioRate);
        int channelCount = 1;
        if (isStereo) {
            channelCount = 2;
        }

        audioInfo[0] = (byte) (16 | soundRateIndex >> 1 & 7);
        audioInfo[1] = (byte) ((soundRateIndex & 1) << 7 | (channelCount & 15) << 3);
        writeAudioTag(buffer, audioInfo, true, audioSize);
    }

    public static void writeAudioTag(ByteBuffer buffer, byte[] audioInfo, boolean isFirst, int audioSize) {
        writeAudioHeader(buffer, isFirst, audioSize);
        buffer.put(audioInfo);
    }

    public static void writeAudioHeader(ByteBuffer buffer, boolean isFirst, int audioSize) {
        int soundFormat = 10;
        int soundRateIndex = 3;
        int soundSize = 1;
        if (audioSize == 8) {
            soundSize = 0;
        }

        int soundType = 1;
        int AACPacketType = 1;
        if (isFirst) {
            AACPacketType = 0;
        }

        byte[] header = new byte[]{(byte) ((byte) (soundFormat & 15) << 4 | (byte) (soundRateIndex & 3) << 2 | (byte) (soundSize & 1) << 1 | (byte) (soundType & 1)), (byte) AACPacketType};
        buffer.put(header);
    }

    public static int getAudioSimpleRateIndex(int audioSampleRate) {
        byte simpleRateIndex;
        switch (audioSampleRate) {
            case 7350:
                simpleRateIndex = 12;
                break;
            case 8000:
                simpleRateIndex = 11;
                break;
            case 11025:
                simpleRateIndex = 10;
                break;
            case 12000:
                simpleRateIndex = 9;
                break;
            case 16000:
                simpleRateIndex = 8;
                break;
            case 22050:
                simpleRateIndex = 7;
                break;
            case 24000:
                simpleRateIndex = 6;
                break;
            case 32000:
                simpleRateIndex = 5;
                break;
            case 44100:
                simpleRateIndex = 4;
                break;
            case 48000:
                simpleRateIndex = 3;
                break;
            case 64000:
                simpleRateIndex = 2;
                break;
            case 88200:
                simpleRateIndex = 1;
                break;
            case 96000:
                simpleRateIndex = 0;
                break;
            default:
                simpleRateIndex = 15;
        }

        return simpleRateIndex;
    }

    public class FlvMetaValueType {
        public static final int NumberType = 0;
        public static final int BooleanType = 1;
        public static final int StringType = 2;
        public static final int ObjectType = 3;
        public static final int MovieClipType = 4;
        public static final int NullType = 5;
        public static final int UndefinedType = 6;
        public static final int ReferenceType = 7;
        public static final int ECMAArrayType = 8;
        public static final int StrictArrayType = 10;
        public static final int DateType = 11;
        public static final int LongStringType = 12;

        public FlvMetaValueType() {
        }
    }

    public class FlvAvcNaluType {
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

        public FlvAvcNaluType() {
        }
    }

    public class FlvMessageType {
        public static final int FLV = 256;

        public FlvMessageType() {
        }
    }

    public class FlvAudioSampleType {
        public static final int MONO = 0;
        public static final int STEREO = 1;

        public FlvAudioSampleType() {
        }
    }

    public class FlvAudioSampleSize {
        public static final int PCM_8 = 0;
        public static final int PCM_16 = 1;

        public FlvAudioSampleSize() {
        }
    }

    public class FlvAudioSampleRate {
        public static final int Reserved = 15;
        public static final int R96000 = 0;
        public static final int R88200 = 1;
        public static final int R64000 = 2;
        public static final int R48000 = 3;
        public static final int R44100 = 4;
        public static final int R32000 = 5;
        public static final int R24000 = 6;
        public static final int R22050 = 7;
        public static final int R16000 = 8;
        public static final int R12000 = 9;
        public static final int R11025 = 10;
        public static final int R8000 = 11;
        public static final int R7350 = 12;

        public FlvAudioSampleRate() {
        }
    }

    public class FlvAacProfile {
        public static final int Reserved = 3;
        public static final int Main = 0;
        public static final int LC = 1;
        public static final int SSR = 2;

        public FlvAacProfile() {
        }
    }

    public class FlvAacObjectType {
        public static final int Reserved = 0;
        public static final int AacMain = 1;
        public static final int AacLC = 2;
        public static final int AacSSR = 3;
        public static final int AacHE = 5;
        public static final int AacHEV2 = 29;

        public FlvAacObjectType() {
        }
    }

    public class FlvAudio {
        public static final int LINEAR_PCM = 0;
        public static final int AD_PCM = 1;
        public static final int MP3 = 2;
        public static final int LINEAR_PCM_LE = 3;
        public static final int NELLYMOSER_16_MONO = 4;
        public static final int NELLYMOSER_8_MONO = 5;
        public static final int NELLYMOSER = 6;
        public static final int G711_A = 7;
        public static final int G711_MU = 8;
        public static final int RESERVED = 9;
        public static final int AAC = 10;
        public static final int SPEEX = 11;
        public static final int MP3_8 = 14;
        public static final int DEVICE_SPECIFIC = 15;

        public FlvAudio() {
        }
    }

    public class FlvVideoCodecID {
        public static final int Reserved = 0;
        public static final int Reserved1 = 1;
        public static final int Reserved2 = 9;
        public static final int Disabled = 8;
        public static final int SorensonH263 = 2;
        public static final int ScreenVideo = 3;
        public static final int On2VP6 = 4;
        public static final int On2VP6WithAlphaChannel = 5;
        public static final int ScreenVideoVersion2 = 6;
        public static final int AVC = 7;

        public FlvVideoCodecID() {
        }
    }

    public class FlvTag {
        public static final int Reserved = 0;
        public static final int Audio = 8;
        public static final int Video = 9;
        public static final int Script = 18;

        public FlvTag() {
        }
    }

    public class FlvAudioAACPacketType {
        public static final int SequenceHeader = 0;
        public static final int Raw = 1;

        public FlvAudioAACPacketType() {
        }
    }

    public class FlvVideoAVCPacketType {
        public static final int Reserved = 3;
        public static final int SequenceHeader = 0;
        public static final int NALU = 1;
        public static final int SequenceHeaderEOF = 2;

        public FlvVideoAVCPacketType() {
        }
    }

    public class FlvVideoFrameType {
        public static final int Reserved = 0;
        public static final int Reserved1 = 6;
        public static final int KeyFrame = 1;
        public static final int InterFrame = 2;
        public static final int DisposableInterFrame = 3;
        public static final int GeneratedKeyFrame = 4;
        public static final int VideoInfoFrame = 5;

        public FlvVideoFrameType() {
        }
    }
}
