package com.xucz.libscreenstream.tools;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class BuffSizeCalculator {
    public BuffSizeCalculator() {
    }

    public static int calculator(int width, int height, int colorFormat) {
        switch (colorFormat) {
            case 17:
            case 19:
            case 21:
            case 842094169:
                return width * height * 3 / 2;
            default:
                return -1;
        }
    }
}
