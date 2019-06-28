package com.xucz.libscreenstream.utils;

import android.database.Cursor;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * <p>Created by LeonLee on 2014/9/4 17:21.</p>
 * IO处理工具类
 * <p>
 * <ur>
 * <li>{@link #closeQuietly(Closeable)} 关闭数据流</li>
 * <li>{@link #closeQuietly(Cursor)} 关闭数据库游标</li>
 * <li>{@link #copyStream(InputStream, OutputStream)} 复制流，默认缓冲为 8 * 1024</li>
 * <li>{@link #copyStream(InputStream, OutputStream, int)} 复制流，自定义缓冲大小</li>
 * <li>{@link #toByteArray(InputStream)} 将input流转为byte数组，自动关闭</li>
 * </ur>
 */
public class IOUtils {
    private IOUtils() {
    }

    static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * 关闭流
     *
     * @param closeable 实现了{@link Closeable} 的类,像{@link InputStream},
     *                  {@link OutputStream}...
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * 关闭数据库的游标
     *
     * @param cursor 数据库游标
     */
    public static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * 将输入流写出到输出流
     *
     * @param is  输入流
     * @param out 输出流
     * @throws IOException
     */
    public static void copyStream(InputStream is, OutputStream out) throws IOException {
        copyStream(is, out, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 将输入流写出到输出流
     *
     * @param is          输入流
     * @param out         输出流
     * @param buffer_size 缓存区大小
     * @throws IOException
     */
    public static void copyStream(InputStream is, OutputStream out, int buffer_size) throws IOException {
        byte[] buffer = new byte[buffer_size];
        int offset = 0;
        while ((offset = is.read(buffer)) != -1) {
            out.write(buffer, 0, offset);
        }
        out.flush();
    }

    /**
     * 将input流转为byte数组，自动关闭
     *
     * @param in 输入流
     * @return
     */
    public static byte[] toByteArray(InputStream in) throws Exception {
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream output = null;
        byte[] result = null;
        try {
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                output.write(buffer, 0, n);
            }
            result = output.toByteArray();
        } finally {
            closeQuietly(in);
            closeQuietly(output);
        }
        return result;
    }

    /**
     * 将输入流转为字符串
     *
     * @param is 输入流
     */
    public static String toString(InputStream is) {
        InputStreamReader reader = null;
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            reader = new InputStreamReader(is);
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(reader);
        }
        return null;
    }

    /**
     * desc:将数组转为16进制
     *
     * @param bArray
     * @return modified:
     */
    public static String bytesToHexString(byte[] bArray) {
        if (bArray == null) {
            return null;
        }
        if (bArray.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * desc:将16进制的数据转为数组
     * <p>创建人：聂旭阳 , 2014-5-25 上午11:08:33</p>
     *
     * @param data
     * @return modified:
     */
    public static byte[] StringToBytes(String data) {
        String hexString = data.toUpperCase().trim();
        if (hexString.length() % 2 != 0) {
            return null;
        }
        byte[] retData = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i++) {
            int int_ch;  // 两位16进制数转化后的10进制数
            char hex_char1 = hexString.charAt(i); ////两位16进制数中的第一位(高位*16)
            int int_ch3;
            if (hex_char1 >= '0' && hex_char1 <= '9')
                int_ch3 = (hex_char1 - 48) * 16;   //// 0 的Ascll - 48
            else if (hex_char1 >= 'A' && hex_char1 <= 'F')
                int_ch3 = (hex_char1 - 55) * 16; //// A 的Ascll - 65
            else
                return null;
            i++;
            char hex_char2 = hexString.charAt(i); ///两位16进制数中的第二位(低位)
            int int_ch4;
            if (hex_char2 >= '0' && hex_char2 <= '9')
                int_ch4 = (hex_char2 - 48); //// 0 的Ascll - 48
            else if (hex_char2 >= 'A' && hex_char2 <= 'F')
                int_ch4 = hex_char2 - 55; //// A 的Ascll - 65
            else
                return null;
            int_ch = int_ch3 + int_ch4;
            retData[i / 2] = (byte) int_ch;//将转化后的数放入Byte里
        }
        return retData;
    }

}
