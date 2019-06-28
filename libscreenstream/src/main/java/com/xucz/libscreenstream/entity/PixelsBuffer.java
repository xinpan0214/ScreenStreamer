package com.xucz.libscreenstream.entity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class PixelsBuffer {
    private ByteBuffer buffer;
    private boolean invalid;

    public static PixelsBuffer allocate(int capacity) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
        buffer.order(ByteOrder.nativeOrder());
        return wrap(buffer);
    }

    public static PixelsBuffer wrap(ByteBuffer buffer) {
        PixelsBuffer pixelsBuffer = new PixelsBuffer();
        pixelsBuffer.buffer = buffer;
        pixelsBuffer.valid();
        return pixelsBuffer;
    }

    private PixelsBuffer() {
    }

    public String toString() {
        return "PixelsBuffer{buffer=" + this.buffer + ", invalid=" + this.invalid + '}';
    }

    public ByteBuffer getBuffer() {
        this.position(0);
        return this.buffer;
    }

    public boolean isInvalid() {
        return this.invalid;
    }

    private void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public void valid() {
        this.setInvalid(false);
    }

    public void invalid() {
        this.setInvalid(true);
    }

    public void position(int position) {
        if (null != this.buffer) {
            this.buffer.position(position);
        }

    }

    public void clear() {
        if (null != this.buffer) {
            this.buffer.clear();
        }

    }
}

