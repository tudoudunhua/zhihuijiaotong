package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.Warning;
import com.zhihui.bishe.repository.ViolationRepository;
import com.zhihui.bishe.repository.WarningRepository;
import com.zhihui.bishe.service.MIPredictService;
import com.zhihui.bishe.service.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intelligent")
public class IntelligentWarningController {

    @Autowired
    private MIPredictService mlPredictService;

    @Autowired
    private WarningService warningService;

    @Autowired
    private ViolationRepository violationRepository;
    
    @Autowired
    private WarningRepository warningRepository;

    /*
     智能违章预警接口
     */
    @GetMapping("/predict")
    public Map<String, Object> predict(
            @RequestParam(required = false) Integer hour,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location
    ) {
        int h = (hour == null) ? 0 : hour;
        String t = (type == null) ? "0" : type;
        String loc = (location == null) ? "0" : location;
        return mlPredictService.predict(h, t, loc);
    }

    /**
     * 手动触发：根据天气规则生成一批智能预警
     * 用于演示或调试，前端可在智能预警页增加“生成天气预警”按钮调用。
     */
    @PostMapping("/trigger-weather")
    public Map<String, Object> triggerWeatherWarnings() {
        Map<String, Object> res = new HashMap<>();
        try {
            warningService.generateWeatherWarningsOnce();
            res.put("status", "ok");
            res.put("message", "已根据示例天气规则生成一批智能预警");
        } catch (Exception e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    // 调试/触发端点：立即执行 ML 预警生成（手动触发）
    @PostMapping("/trigger-ml")
    public Map<String, Object> triggerML() {
        Map<String, Object> res = new HashMap<>();
        try {
            // 在后台线程异步执行，避免 HTTP 请求超时
            new Thread(() -> {
                try {
                    warningService.generateMLWarnings();
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }).start();
            res.put("status", "accepted");
        } catch (Exception e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    // 返回可选的违章类型列表（用于前端下拉）
    @GetMapping("/types")
    public List<String> getTypes() {
        List<String> types = violationRepository.findDistinctTypes();
        if (types == null || types.isEmpty()) {
            java.io.File csv = new java.io.File(System.getProperty("user.dir"), "traffic_violation.csv");
            java.util.Set<String> set = new java.util.LinkedHashSet<>();
            if (csv.exists()) {
                // 尝试 UTF-8，若结果看起来有乱码则改用 GB18030 重读
                java.util.List<String> read = readColumnFromCsv(csv, "violation_type", java.nio.charset.StandardCharsets.UTF_8.name());
                if (read.isEmpty() || read.stream().anyMatch(s -> s.contains("�"))) {
                    read = readColumnFromCsv(csv, "violation_type", "GB18030");
                }
                set.addAll(read);
            }
            return new java.util.ArrayList<>(set);
        }
        return types;
    }
    
    // 返回行政区下拉候选（从 violation_location 提取区/县/街道等）
    @GetMapping("/districts")
    public List<String> getDistricts() {
        List<String> locs = violationRepository.findDistinctLocations();
        java.util.Set<String> districts = new java.util.LinkedHashSet<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("市(.+?[区县市街道镇])");

        // 如果数据库中没有数据，则回退读取项目根目录下的 traffic_violation.csv
        if (locs == null || locs.isEmpty()) {
            java.io.File csv = new java.io.File(System.getProperty("user.dir"), "traffic_violation.csv");
            if (csv.exists()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(csv), java.nio.charset.StandardCharsets.UTF_8))) {
                    String header = br.readLine();
                    if (header != null) {
                        // 找到 violation_location 列索引（不区分大小写）
                        String[] heads = header.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                        int locIdx = -1;
                        for (int i = 0; i < heads.length; i++) {
                            if (heads[i].trim().replaceAll("\"", "").equalsIgnoreCase("violation_location")) {
                                locIdx = i;
                                break;
                            }
                        }
                        String line;
                        while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                            if (locIdx >= 0 && locIdx < parts.length) {
                                String loc = parts[locIdx].trim();
                                if (loc.startsWith("\"") && loc.endsWith("\"")) {
                                    loc = loc.substring(1, loc.length() - 1);
                                }
                                if (loc.isEmpty()) continue;
                                java.util.regex.Matcher m = p.matcher(loc);
                                if (m.find()) {
                                    districts.add(m.group(1));
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            for (String loc : locs) {
                if (loc == null) continue;
                java.util.regex.Matcher m = p.matcher(loc);
                if (m.find()) {
                    districts.add(m.group(1));
                }
            }
        }

        return new java.util.ArrayList<>(districts);
    }

    // Helper: 从 CSV 中读取某一列，返回值列表（不去重）
    private java.util.List<String> readColumnFromCsv(java.io.File csv, String columnName, String charsetName) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(csv), java.nio.charset.Charset.forName(charsetName))
        )) {
            String header = br.readLine();
            if (header == null) return out;
            String[] heads = header.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            int idx = -1;
            for (int i = 0; i < heads.length; i++) {
                if (heads[i].trim().replaceAll("\"", "").equalsIgnoreCase(columnName)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) return out;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (idx < parts.length) {
                    String v = parts[idx].trim();
                    if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                    if (!v.isEmpty()) {
                        // 修复可能的编码错解（例如文件实际为 GBK，但被当作 UTF-8 读取）
                        if (!containsCJK(v)) {
                            try {
                                String fixed = new String(v.getBytes("ISO-8859-1"), "GB18030");
                                if (containsCJK(fixed)) v = fixed;
                            } catch (Exception ignore) {}
                        }
                        out.add(v);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return out;
    }

    private boolean containsCJK(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) return true;
        }
        return false;
    }
    
    // 返回最近生成的预警列表（按时间降序）
    @GetMapping("/latest-warnings")
    public List<Warning> latestWarnings() {
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("warningTime").descending());
        return warningRepository.findAll(p).getContent();
    }

    /**
     * 复犯车辆预警：近30天内重复违章车牌
     */
    @GetMapping("/repeat-offenders")
    public List<Map<String, Object>> repeatOffenders(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minCount
    ) {
        String loc = (location == null || location.trim().isEmpty()) ? "0" : location.trim();
        long threshold = (minCount == null || minCount < 2) ? 2L : minCount;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
        java.util.Date since = cal.getTime();

        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 20);
        List<Object[]> rows = violationRepository.findRepeatOffendersSince(since, loc, threshold, p);

        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new HashMap<>();
            String plate = String.valueOf(row[0]);
            int total = ((Number) row[1]).intValue();
            int typeCount = ((Number) row[2]).intValue();
            Object lastTime = row[3];

            String level;
            if (total >= 6) {
                level = "高";
            } else if (total >= 4) {
                level = "中";
            } else {
                level = "低";
            }

            m.put("plateNumber", plate);
            m.put("totalCount", total);
            m.put("typeDiversity", typeCount);
            m.put("lastViolationTime", lastTime);
            m.put("riskLevel", level);
            out.add(m);
        }
        return out;
    }
}
