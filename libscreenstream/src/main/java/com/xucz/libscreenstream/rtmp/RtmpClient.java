package com.xucz.libscreenstream.rtmp;

import com.xucz.libscreenstream.log.PushLog;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class RtmpClient {
    public RtmpClient() {
    }

    public static native boolean init(String var0, int var1);

    public static native int connect();

    public static native void closeConnection();

    public static native void stop(boolean var0);

    public static native boolean createNetStream();

    public static native boolean closeNetStream();

    public static native boolean sendVideoSequenceHeader(byte[] var0, int var1, byte[] var2, int var3);

    public static native boolean sendVideoPacket(byte[] var0, int var1, int var2, boolean var3);

    public static native boolean sendAsusVideoPacket(byte[] var0, int var1, int var2, boolean var3);

    public static native boolean sendAudioSequenceHeader(int var0, int var1);

    public static native boolean sendAudioPacket(byte[] var0, int var1, int var2, boolean var3);

    public static native void setTimeout(int var0, int var1);

    public static native void parseQuicConfig(int var0, int var1, int var2, int var3, int var4, int var5);

    private static native void sendCodecType(int var0, double var1, double var3, double var5, double var7);

    public static void sendEncoderCodecType(boolean soft, double width, double height, double videoFrameRate, double videoBitrate) {
        PushLog.i("sendEncoderCodecType encoder is soft=" + soft + ",w=" + width + ",h=" + height);
        if (soft) {
            sendCodecType(1, width, height, videoFrameRate, videoBitrate);
        } else {
            sendCodecType(2, width, height, videoFrameRate, videoBitrate);
        }

    }

    static {
        System.loadLibrary("nonoproto");
        System.loadLibrary("nonortmp");
        System.loadLibrary("livepush");
    }
}
