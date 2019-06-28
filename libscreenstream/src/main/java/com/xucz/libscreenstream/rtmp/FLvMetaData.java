package com.xucz.libscreenstream.rtmp;

import com.xucz.libscreenstream.config.Configure;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class FLvMetaData {
    private static final String Name = "onMetaData";
    private static final int ScriptData = 18;
    private static final byte[] TS_SID = new byte[]{0, 0, 0, 0, 0, 0, 0};
    private static final byte[] ObjEndMarker = new byte[]{0, 0, 9};
    private static final int EmptySize = 21;
    private ArrayList<byte[]> MetaData;
    private int DataSize;
    private int pointer;
    private byte[] MetaDataFrame;

    public FLvMetaData() {
        this.MetaData = new ArrayList();
        this.DataSize = 0;
    }

    public FLvMetaData(Configure configure) {
        this();
        this.setProperty("audiocodecid", 10);
        switch (65536) {
            case 32768:
                this.setProperty("audiodatarate", 32);
                break;
            case 49152:
                this.setProperty("audiodatarate", 48);
                break;
            case 65536:
                this.setProperty("audiodatarate", 64);
        }

        switch (Configure.audioConfig.getFixedSampleRates()) {
            case 44100:
                this.setProperty("audiosamplerate", 44100);
            default:
                this.setProperty("videocodecid", 7);
                this.setProperty("framerate", 18);
                this.setProperty("width", configure.videoWidth);
                this.setProperty("height", configure.videoHeight);
        }
    }

    public void setProperty(String Key, int value) {
        this.addProperty(this.toFlvString(Key), (byte) 0, this.toFlvNum((double) value));
    }

    public void setProperty(String Key, String value) {
        this.addProperty(this.toFlvString(Key), (byte) 2, this.toFlvString(value));
    }

    private void addProperty(byte[] Key, byte datatype, byte[] data) {
        int Propertysize = Key.length + 1 + data.length;
        byte[] Property = new byte[Propertysize];
        System.arraycopy(Key, 0, Property, 0, Key.length);
        Property[Key.length] = datatype;
        System.arraycopy(data, 0, Property, Key.length + 1, data.length);
        this.MetaData.add(Property);
        this.DataSize += Propertysize;
    }

    public byte[] getMetaData() {
        this.MetaDataFrame = new byte[this.DataSize + 21];
        this.pointer = 0;
        this.Addbyte(2);
        this.AddbyteArray(this.toFlvString("onMetaData"));
        this.Addbyte(8);
        this.AddbyteArray(this.toUI((long) this.MetaData.size(), 4));
        Iterator var1 = this.MetaData.iterator();

        while (var1.hasNext()) {
            byte[] Property = (byte[]) var1.next();
            this.AddbyteArray(Property);
        }

        this.AddbyteArray(ObjEndMarker);
        return this.MetaDataFrame;
    }

    private void Addbyte(int value) {
        this.MetaDataFrame[this.pointer] = (byte) value;
        ++this.pointer;
    }

    private void AddbyteArray(byte[] value) {
        System.arraycopy(value, 0, this.MetaDataFrame, this.pointer, value.length);
        this.pointer += value.length;
    }

    private byte[] toFlvString(String text) {
        byte[] FlvString = new byte[text.length() + 2];
        System.arraycopy(this.toUI((long) text.length(), 2), 0, FlvString, 0, 2);
        System.arraycopy(text.getBytes(), 0, FlvString, 2, text.length());
        return FlvString;
    }

    private byte[] toUI(long value, int bytes) {
        byte[] UI = new byte[bytes];

        for (int i = 0; i < bytes; ++i) {
            UI[bytes - 1 - i] = (byte) ((int) (value >> 8 * i & 255L));
        }

        return UI;
    }

    private byte[] toFlvNum(double value) {
        long tmp = Double.doubleToLongBits(value);
        return this.toUI(tmp, 8);
    }
}
