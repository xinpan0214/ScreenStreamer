package com.xucz.libscreenstream.tools;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class ByteArrayTools {
    public ByteArrayTools() {
    }

    public static void intToByteArrayFull(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) (interger >> 24 & 255);
        dst[pos + 1] = (byte) (interger >> 16 & 255);
        dst[pos + 2] = (byte) (interger >> 8 & 255);
        dst[pos + 3] = (byte) (interger & 255);
    }

    public static void intToByteArrayTwoByte(byte[] dst, int pos, int interger) {
        dst[pos] = (byte) (interger >> 8 & 255);
        dst[pos + 1] = (byte) (interger & 255);
    }
}
