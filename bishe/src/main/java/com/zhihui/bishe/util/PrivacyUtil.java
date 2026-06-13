package com.zhihui.bishe.util;

/**
 * 数据脱敏工具（课题要求：对外展示车牌为 京A****5，不暴露完整信息）
 */
public final class PrivacyUtil {

    /**
     * 车牌脱敏：保留首字符与最后一位，中间用 * 代替。
     * 例如：京A12345 -> 京A****5；豫B88888 -> 豫B****8
     */
    public static String maskPlateNumber(String plateNumber) {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return "";
        }
        String s = plateNumber.trim();
        if (s.length() <= 2) {
            return s;
        }
        if (s.length() == 3) {
            return s.charAt(0) + "**" + s.charAt(2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(0));
        sb.append(s.charAt(1));
        for (int i = 2; i < s.length() - 1; i++) {
            sb.append('*');
        }
        sb.append(s.charAt(s.length() - 1));
        return sb.toString();
    }
}
