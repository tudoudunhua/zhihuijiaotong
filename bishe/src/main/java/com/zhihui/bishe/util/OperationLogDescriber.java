package com.zhihui.bishe.util;

import org.springframework.util.AntPathMatcher;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 API 请求映射为可读的中文操作说明。
 */
public final class OperationLogDescriber {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final Map<String, String[]> RULES = new LinkedHashMap<>();

    static {
        RULES.put("POST:/api/auth/login", new String[]{"AUTH", "用户登录"});

        RULES.put("POST:/api/users", new String[]{"USER", "新增用户"});
        RULES.put("PUT:/api/users/*/permissions", new String[]{"USER", "修改用户权限"});
        RULES.put("DELETE:/api/users/*", new String[]{"USER", "删除用户"});

        RULES.put("POST:/api/violations", new String[]{"VIOLATION", "新增违章记录"});
        RULES.put("PUT:/api/violations/*", new String[]{"VIOLATION", "更新违章记录"});
        RULES.put("DELETE:/api/violations/*", new String[]{"VIOLATION", "删除违章记录"});
        RULES.put("POST:/api/violations/*/process", new String[]{"VIOLATION", "标记违章已处理"});
        RULES.put("POST:/api/violations/*/notify", new String[]{"VIOLATION", "通知车主"});
        RULES.put("POST:/api/violations/*/image", new String[]{"VIOLATION", "上传违章照片"});
        RULES.put("POST:/api/violations/*/auto-image", new String[]{"VIOLATION", "生成示例违章照片"});

        RULES.put("POST:/api/cameras", new String[]{"CAMERA", "新增摄像头"});
        RULES.put("PUT:/api/cameras/*", new String[]{"CAMERA", "更新摄像头"});
        RULES.put("DELETE:/api/cameras/*", new String[]{"CAMERA", "删除摄像头"});

        RULES.put("POST:/api/owners", new String[]{"OWNER", "录入/更新车主信息"});

        RULES.put("POST:/api/simulate/captured-violations", new String[]{"SIMULATE", "模拟摄像头抓拍违章"});
        RULES.put("POST:/api/simulate/demo-warnings", new String[]{"SIMULATE", "生成演示预警数据"});

        RULES.put("POST:/api/intelligent/trigger-weather", new String[]{"WARNING", "触发天气智能预警"});
        RULES.put("POST:/api/intelligent/trigger-ml", new String[]{"WARNING", "触发机器学习预警"});

        RULES.put("POST:/api/admin/import-sql", new String[]{"SYSTEM", "导入 SQL 数据"});
    }

    private OperationLogDescriber() {
    }

    public static DescribedOperation describe(String httpMethod, String requestUri) {
        String method = httpMethod == null ? "" : httpMethod.toUpperCase();
        String uri = normalizeUri(requestUri);
        String key = method + ":" + uri;

        for (Map.Entry<String, String[]> entry : RULES.entrySet()) {
            String pattern = entry.getKey();
            int colon = pattern.indexOf(':');
            String ruleMethod = pattern.substring(0, colon);
            String rulePath = pattern.substring(colon + 1);
            if (!ruleMethod.equalsIgnoreCase(method)) {
                continue;
            }
            if (MATCHER.match(rulePath, uri)) {
                String[] v = entry.getValue();
                return new DescribedOperation(v[0], v[1]);
            }
        }

        String module = guessModule(uri);
        String action = method + " " + uri;
        return new DescribedOperation(module, action);
    }

    private static String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        int q = uri.indexOf('?');
        if (q >= 0) {
            uri = uri.substring(0, q);
        }
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return uri;
    }

    private static String guessModule(String uri) {
        if (uri.startsWith("/api/auth")) return "AUTH";
        if (uri.startsWith("/api/users")) return "USER";
        if (uri.startsWith("/api/violations")) return "VIOLATION";
        if (uri.startsWith("/api/cameras")) return "CAMERA";
        if (uri.startsWith("/api/warnings") || uri.startsWith("/api/intelligent")) return "WARNING";
        if (uri.startsWith("/api/simulate")) return "SIMULATE";
        if (uri.startsWith("/api/owners")) return "OWNER";
        if (uri.startsWith("/api/admin")) return "SYSTEM";
        return "OTHER";
    }

    public static final class DescribedOperation {
        private final String module;
        private final String action;

        public DescribedOperation(String module, String action) {
            this.module = module;
            this.action = action;
        }

        public String getModule() {
            return module;
        }

        public String getAction() {
            return action;
        }
    }
}
