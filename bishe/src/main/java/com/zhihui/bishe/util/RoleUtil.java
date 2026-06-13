package com.zhihui.bishe.util;

/**
 * 统一角色编码：兼容中文角色名与英文枚举。
 */
public final class RoleUtil {

    private RoleUtil() {
    }

    public static String normalize(String role) {
        if (role == null) {
            return "USER";
        }
        String r = role.trim();
        if (r.isEmpty()) {
            return "USER";
        }
        String upper = r.toUpperCase();
        if ("管理员".equals(r) || "ADMIN".equalsIgnoreCase(r)) {
            return "ADMIN";
        }
        if ("交警处置员".equals(r) || "POLICE".equalsIgnoreCase(r) || "OFFICER".equalsIgnoreCase(r)) {
            return "OFFICER";
        }
        if ("数据分析员".equals(r) || "ANALYSIS".equalsIgnoreCase(r) || "ANALYST".equalsIgnoreCase(r)) {
            return "ANALYST";
        }
        if ("路段网格员".equals(r) || "ROAD_GRID".equalsIgnoreCase(r)) {
            return "ROAD_GRID";
        }
        if ("网格员".equals(r) || "GRID_MEMBER".equalsIgnoreCase(r)) {
            return "GRID_MEMBER";
        }
        if ("交警中队".equals(r) || "TRAFFIC_SQUAD".equalsIgnoreCase(r)) {
            return "TRAFFIC_SQUAD";
        }
        if ("消防".equals(r) || "FIRE".equalsIgnoreCase(r)) {
            return "FIRE";
        }
        if ("USER".equalsIgnoreCase(r) || "普通用户".equals(r)) {
            return "USER";
        }
        return upper;
    }

    public static boolean isAdmin(String role) {
        return "ADMIN".equals(normalize(role));
    }
}
