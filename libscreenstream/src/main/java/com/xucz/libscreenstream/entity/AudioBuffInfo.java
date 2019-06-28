package com.xucz.libscreenstream.entity;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class AudioBuffInfo {
    public boolean isReadyToFill = true;
    public int audioFormat = -1;
    public byte[] buff;

    public AudioBuffInfo(int audioFormat, int size) {
        this.audioFormat = audioFormat;
        this.buff = new byte[size];
    }
}

