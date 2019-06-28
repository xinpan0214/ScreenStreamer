package com.xucz.libscreenstream.rtmp;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class FlvData {
    public static final int FLV_RTMP_PACKET_TYPE_VIDEO = 9;
    public static final int FLV_RTMP_PACKET_TYPE_AUDIO = 8;
    public static final int FLV_RTMP_PACKET_TYPE_INFO = 18;
    public static final int NALU_TYPE_IDR = 5;
    public int dts;
    public byte[] byteBuffer;
    public int size;
    public int flvTagType;
    public int videoFrameType;

    public FlvData() {
    }

    public boolean isKeyframe() {
        return this.videoFrameType == 5;
    }
}

