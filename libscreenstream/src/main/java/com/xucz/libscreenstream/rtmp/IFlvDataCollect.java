package com.xucz.libscreenstream.rtmp;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public interface IFlvDataCollect {
    int ERROR_UNKONWN = 0;
    int ERROR_HIGH_PROFILE = 1;
    int FROM_AUDIO = 8;
    int FROM_VIDEO = 6;

    void onVideoSequenceHeader(byte[] var1, int var2, byte[] var3, int var4, boolean var5);

    void onVideoPacket(byte[] var1, int var2, int var3, boolean var4);

    void onAudioSequenceHeader();

    void onAudioPacket(byte[] var1, int var2, int var3, boolean var4);

    void onSoftEncodeDetectFinished(boolean var1);

    void onChangeToHardEncoder();

    void onFirstDrawScreen();

    void onVideoEncodeError(int var1);
}
