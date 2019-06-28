package com.xucz.libscreenstream.utils;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class StringUtils {
    private StringUtils() {
    }

    public static String getRandomString(int length) {
        String str = "abcdefghigklmnopkrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random(System.currentTimeMillis());
        StringBuffer sbf = new StringBuffer();

        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(62);
            sbf.append(str.charAt(number));
        }

        return sbf.toString();
    }

    public static String makeGUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static String[] split(String src, String delimiter) {
        if (src != null && delimiter != null && !src.trim().equals("") && !delimiter.trim().equals("")) {
            ArrayList<String> list = new ArrayList();
            int lengthOfDelimiter = delimiter.length();
            int pos = 0;

            while (true) {
                if (pos >= src.length()) {
                    if (pos == src.length()) {
                        list.add("");
                    }
                    break;
                }

                int indexOfDelimiter = src.indexOf(delimiter, pos);
                if (indexOfDelimiter < 0) {
                    list.add(src.substring(pos));
                    break;
                }

                list.add(src.substring(pos, indexOfDelimiter));
                pos = indexOfDelimiter + lengthOfDelimiter;
            }

            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        } else {
            return new String[]{src};
        }
    }

    public static String errEncode(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        } else {
            Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5\\u0800-\\u4e00]+");
            Matcher matcher = pattern.matcher(s);
            if (!matcher.find()) {
                try {
                    return new String(s.getBytes("iso-8859-1"), "GBK");
                } catch (UnsupportedEncodingException var4) {
                }
            }

            return s;
        }
    }

    public static boolean isErrCode(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        } else {
            Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]+");
            Matcher matcher = pattern.matcher(s);
            return !matcher.find();
        }
    }

    public static String add0IfLgTen(int i) {
        return 0 < i && i < 10 ? "0" + i + "." : i + ".";
    }

    public static String getSizeText(long fileSize) {
        if (fileSize <= 0L) {
            return "0.0M";
        } else if (fileSize > 0L && fileSize < 102400L) {
            return "0.1M";
        } else {
            float result = (float) fileSize;
            String suffix = "M";
            result = result / 1024.0F / 1024.0F;
            return String.format("%.1f", result) + suffix;
        }
    }

    public static String getSizeText(Context context, long bytes) {
        String sizeText = "";
        if (bytes < 0L) {
            return sizeText;
        } else {
            sizeText = Formatter.formatFileSize(context, bytes);
            return sizeText;
        }
    }

    public static String spiltImageName(String imageurl) {
        if (TextUtils.isEmpty(imageurl)) {
            return null;
        } else {
            imageurl = imageurl.toLowerCase();
            int start = imageurl.lastIndexOf("filename");
            if (start == -1) {
                start = imageurl.lastIndexOf("/");
                if (start == -1) {
                    return null;
                }

                ++start;
            } else {
                start += 9;
            }

            int end = imageurl.indexOf(".jpg", start);
            if (end == -1) {
                end = imageurl.indexOf(".png", start);
                if (end == -1) {
                    return null;
                }

                end += 4;
            } else {
                end += 4;
            }

            return imageurl.substring(start, end);
        }
    }

    public static String hashImageName(String imageUrl, String hash) {
        if (!TextUtils.isEmpty(imageUrl) && !TextUtils.isEmpty(hash)) {
            imageUrl = imageUrl.toLowerCase();
            hash = hash.toLowerCase();
            int index = imageUrl.indexOf(".jpg");
            if (index == -1) {
                index = imageUrl.indexOf(".png");
            }

            return hash + imageUrl.substring(index);
        } else {
            return null;
        }
    }

    public static String getExceptionString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString().replace("\n", "<br />");
    }

    public static String subStrByByteLen(String original, String charsetName, int byteLen, boolean isRightTruncation) throws UnsupportedEncodingException {
        if (original != null && !"".equals(original.trim())) {
            if (charsetName != null && !"".equals(charsetName)) {
                byte[] bytes = original.getBytes(charsetName);
                if (byteLen <= 0) {
                    return "";
                } else if (byteLen >= bytes.length) {
                    return original;
                } else {
                    String result = "";
                    int tempLen;
                    if (isRightTruncation) {
                        tempLen = (new String(bytes, 0, byteLen, charsetName)).length();
                        result = original.substring(0, tempLen);
                        if (result != null && !"".equals(result.trim()) && result.getBytes(charsetName).length > byteLen) {
                            result = original.substring(0, tempLen - 1);
                        }
                    } else {
                        tempLen = (new String(bytes, 0, bytes.length - byteLen + 1, charsetName)).length() - 1;
                        result = original.substring(tempLen);
                        if (result != null && !"".equals(result.trim()) && result.getBytes(charsetName).length > byteLen) {
                            result = original.substring(tempLen + 1);
                        }
                    }

                    return result;
                }
            } else {
                throw new UnsupportedEncodingException("must set CharsetName for function: subStrByByteLen()");
            }
        } else {
            return "";
        }
    }

    public static int countWords(String str) {
        int len = 0;

        try {
            if (!TextUtils.isEmpty(str)) {
                len = str.getBytes("GBK").length;
            }
        } catch (UnsupportedEncodingException var3) {
            var3.printStackTrace();
        }

        return len;
    }

    public static boolean versionOver(String v1, String v2) {
        if (!TextUtils.isEmpty(v1) && !TextUtils.isEmpty(v2)) {
            if (v1.startsWith("v")) {
                v1 = v1.substring(1);
            }

            if (v2.startsWith("v")) {
                v2 = v2.substring(1);
            }

            String[] vs1 = v1.split("\\.");
            String[] vs2 = v2.split("\\.");
            int len = vs1.length;
            len = len < vs2.length ? len : vs2.length;

            for (int i = 0; i < len; ++i) {
                try {
                    int vi1 = Integer.parseInt(vs1[i]);
                    int vi2 = Integer.parseInt(vs2[i]);
                    if (vi1 < vi2) {
                        return false;
                    }

                    if (vi1 > vi2) {
                        return true;
                    }
                } catch (NumberFormatException var8) {
                    return false;
                }
            }

            if (vs1.length < vs2.length) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static String formatTime(DateFormat dateFormat, long time) {
        if (String.valueOf(time).length() < 13) {
            time *= 1000L;
        }

        return dateFormat.format(new Date(time));
    }

    public static ArrayList<String> splitString(String strNameLst, String split) {
        if (TextUtils.isEmpty(strNameLst)) {
            return null;
        } else {
            String[] array = split(strNameLst, split);
            if (array != null && array.length != 0) {
                ArrayList<String> temp = new ArrayList();
                String[] var4 = array;
                int var5 = array.length;

                for (int var6 = 0; var6 < var5; ++var6) {
                    String string = var4[var6];
                    temp.add(string);
                }

                return temp;
            } else {
                return null;
            }
        }
    }

    public static String mergeString(List<String> stringLst, String split) {
        StringBuilder sb = new StringBuilder();
        int len = stringLst.size();

        for (int i = 0; i < len; ++i) {
            sb.append((String) stringLst.get(i));
            if (i < len - 1) {
                sb.append(split);
            }
        }

        return sb.toString();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; ++i) {
            String hex = Integer.toHexString(255 & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }

            sb.append(hex);
        }

        return sb.toString();
    }

    public static boolean isNotEmpty(CharSequence text) {
        return text != null && text.length() > 0;
    }

    public static boolean isEmpty(CharSequence text) {
        return text == null || text.length() == 0;
    }

    public static String formatFilePath(String filePath) {
        return TextUtils.isEmpty(filePath) ? null : filePath.replace("\\", "").replace("/", "").replace("*", "").replace("?", "").replace(":", "").replace("\"", "").replace("<", "").replace(">", "").replace("|", "");
    }

    public static String formatUserName(String userName) {
        if (TextUtils.isEmpty(userName)) {
            return userName;
        } else {
            userName = userName.replaceAll(" +", " ");
            userName = userName.replaceAll("(\r?\n(\\s*\r?\n)+)", "");
            return userName;
        }
    }

    public static String formatChatMessage(String chatMessage) {
        if (TextUtils.isEmpty(chatMessage)) {
            return chatMessage;
        } else {
            chatMessage = chatMessage.replaceAll(" +", " ");
            chatMessage = chatMessage.replaceAll("(\r?\n(\\s*\r?\n)+)", "\n");
            return chatMessage;
        }
    }

    public static String formatAndLimitUserName(String userName) {
        userName = formatUserName(userName);
        if (userName != null && userName.length() > 16) {
            userName = userName.substring(0, 16) + "...";
        }

        return userName;
    }
}

