package com.xucz.libscreenstream.rtmp;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class ByteSpeedometer {
    private int timeGranularity;
    private LinkedList<ByteFrame> byteList;
    private final Object syncByteList = new Object();

    public ByteSpeedometer(int timeGranularity) {
        this.timeGranularity = timeGranularity;
        this.byteList = new LinkedList();
    }

    public int getSpeed() {
        synchronized (this.syncByteList) {
            long now = System.currentTimeMillis();
            this.trim(now);
            long sumByte = 0L;

            ByteSpeedometer.ByteFrame byteFrame;
            for (Iterator var6 = this.byteList.iterator(); var6.hasNext(); sumByte += byteFrame.bytenum) {
                byteFrame = (ByteSpeedometer.ByteFrame) var6.next();
            }

            return (int) (sumByte * 1000L / (long) this.timeGranularity);
        }
    }

    public void gain(int byteCount) {
        synchronized (this.syncByteList) {
            long now = System.currentTimeMillis();
            this.byteList.addLast(new ByteSpeedometer.ByteFrame(now, (long) byteCount));
            this.trim(now);
        }
    }

    private void trim(long time) {
        while (!this.byteList.isEmpty() && time - ((ByteSpeedometer.ByteFrame) this.byteList.getFirst()).time > (long) this.timeGranularity) {
            this.byteList.removeFirst();
        }

    }

    public void reset() {
        synchronized (this.syncByteList) {
            this.byteList.clear();
        }
    }

    private class ByteFrame {
        long time;
        long bytenum;

        public ByteFrame(long time, long bytenum) {
            this.time = time;
            this.bytenum = bytenum;
        }
    }
}
