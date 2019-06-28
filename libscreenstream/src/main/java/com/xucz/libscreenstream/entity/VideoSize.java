package com.xucz.libscreenstream.entity;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public final class VideoSize {
    private final int mWidth;
    private final int mHeight;

    public VideoSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (!(obj instanceof VideoSize)) {
            return false;
        } else {
            VideoSize other = (VideoSize) obj;
            return this.mWidth == other.mWidth && this.mHeight == other.mHeight;
        }
    }

    public String toString() {
        return this.mWidth + "x" + this.mHeight;
    }

    private static NumberFormatException invalidSize(String s) {
        throw new NumberFormatException("Invalid Size: \"" + s + "\"");
    }

    public int hashCode() {
        return this.mHeight ^ (this.mWidth << 16 | this.mWidth >>> 16);
    }
}
