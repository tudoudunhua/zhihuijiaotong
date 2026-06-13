package com.zhihui.bishe.service;

import com.zhihui.bishe.model.Warning;
import com.zhihui.bishe.repository.ViolationRepository;
import com.zhihui.bishe.repository.WarningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@org.springframework.scheduling.annotation.EnableScheduling
public class WarningService {

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private WarningRepository warningRepository;
    
    @Autowired
    private MIPredictService mlPredictService;

    // 用于天气预警的示例天气类型与高风险区域（用于演示和论文场景）
    private static final List<String> WEATHER_TYPES = Arrays.asList(
            "大雨", "暴雨", "大雾", "雨夹雪", "冰雪路面"
    );

    private static final List<String> WEATHER_LOCATIONS = Arrays.asList(
            "金水区-花园路",
            "二七区-大学路",
            "中原区-建设路",
            "郑东新区-金水东路",
            "管城回族区-航海东路",
            "惠济区-文化北路"
    );

    /** 最新预警列表（用于前端展示与四级推送说明） */
    public List<Warning> getLatestWarnings() {
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, 200,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "warningTime"));
        return warningRepository.findAllByOrderByWarningTimeDesc(page).getContent();
    }

    /** 按用户角色过滤“推送给我”的预警 */
    public List<Warning> getLatestWarningsByRole(String role) {
        List<Warning> all = getLatestWarnings();
        String r = role == null ? "" : role.toUpperCase();
        if (r.isEmpty() || "ADMIN".equals(r)) {
            return all;
        }
        List<Warning> out = new ArrayList<>();
        for (Warning w : all) {
            String target = w.getPushTarget() == null ? "" : w.getPushTarget();
            if ("ROAD_GRID".equals(r) || "ROAD_GRIDER".equals(r)) {
                if (target.contains("路段网格员")) out.add(w);
            } else if ("GRID_MEMBER".equals(r) || "GRID".equals(r)) {
                if (target.contains("网格员") || target.contains("片区负责人")) out.add(w);
            } else if ("TRAFFIC_SQUAD".equals(r) || "OFFICER".equals(r)) {
                if (target.contains("交警中队")) out.add(w);
            } else if ("FIRE".equals(r) || "FIREFIGHTER".equals(r)) {
                if (target.contains("消防")) out.add(w);
            } else if ("ANALYST".equals(r)) {
                out.add(w);
            } else {
                // 普通用户只看低级提醒（可按需调整）
                if ("BLUE".equalsIgnoreCase(w.getLevel())) out.add(w);
            }
        }
        return out;
    }

    /**
     * 早晚高峰违章高发预警
     */
    public List<Warning> generatePeakHourWarnings() {
        List<Warning> warnings = new ArrayList<>();

        // 今日日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // ========= 早高峰 =========
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        Date morningStart = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        Date morningEnd = calendar.getTime();

        // ========= 晚高峰 =========
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        Date eveningStart = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        Date eveningEnd = calendar.getTime();

        // 阈值（可写进论文：经验阈值法）
        long threshold = 5L;

        // 查询早高峰
        warnings.addAll(
                buildWarnings(
                        violationRepository.findPeakHourViolations(
                                morningStart, morningEnd, threshold),
                        "早高峰"
                )
        );

        // 查询晚高峰
        warnings.addAll(
                buildWarnings(
                        violationRepository.findPeakHourViolations(
                                eveningStart, eveningEnd, threshold),
                        "晚高峰"
                )
        );

        return warnings;
    }

    /**
     * 统一生成 Warning 对象
     */
    private List<Warning> buildWarnings(List<Object[]> results, String peakType) {
        List<Warning> warnings = new ArrayList<>();

        for (Object[] row : results) {
            String location = (String) row[0];
            Long count = (Long) row[1];
            int cnt = count.intValue();

            Warning warning = new Warning();
            warning.setWarningType("PeakHour");
            warning.setLocation(location);
            warning.setCount(cnt);
            warning.setSeverity(cnt > 10 ? "High" : "Medium");
            applyLevelAndPushTarget(warning, warning.getSeverity(), cnt);
            warning.setMessage(
                    peakType + "期间【" + location + "】违章高发，次数：" + count
            );

            warningRepository.save(warning);
            warnings.add(warning);
        }
        return warnings;
    }

    /**
     * 使用机器学习模型进行自动化预测并保存预警
     *
     * 逻辑：
     * - 取近期常见地点（从 violationRepository 聚合 top N 地点）
     * - 对未来若干小时（0-23 或指定范围）调用 mlPredictService.predict(hour, type, location)
     * - 如果模型预测的 count 大于阈值则生成 Warning 并保存
     *
     * 可通过 @Scheduled 定时触发（示例：每天/小时）
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void generateMLWarnings() {
        // 选择 top 地点（最近 30 天统计）
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date since = cal.getTime();

        // 这里复用 violationRepository（简单聚合 SQL），若数据量大可改为原生查询
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, 20);
        List<Object[]> topLocations = violationRepository.findTopLocationsSince(since, page);
        long threshold = 5L;

        for (Object[] row : topLocations) {
            String location = (String) row[0];
            // 可扩展：遍历常见类型列表，目前使用常见违章类型集合
            List<String> commonTypes = Arrays.asList("闯红灯", "超速行驶", "违法停车", "酒驾", "未系安全带", "违法变道");
            for (int hour = 0; hour < 24; hour++) {
                for (String type : commonTypes) {
                    try {
                        Map<String, Object> result = mlPredictService.predict(hour, type, location);
                        // 约定：模型返回 times/locations 中包含 count 字段，我们取 locations 首项作为地点预测值
                        Object locsObj = result.get("locations");
                        if (locsObj instanceof List) {
                            List<?> locs = (List<?>) locsObj;
                            if (!locs.isEmpty() && locs.get(0) instanceof Map) {
                                Map<?,?> first = (Map<?,?>) locs.get(0);
                                Number cnt = (Number) first.get("count");
                                long count = (cnt == null) ? 0L : cnt.longValue();
                                if (count >= threshold) {
                                    Warning warning = new Warning();
                                    warning.setWarningType("ML");
                                    warning.setLocation(location);
                                    warning.setViolationType(type);
                                    warning.setCount((int)count);
                                    warning.setSeverity(count > 8 ? "High" : "Medium");
                                    applyLevelAndPushTarget(warning, warning.getSeverity(), (int) count);
                                    warning.setMessage("ML 预测：小时 " + hour + "，类型 " + type + "，地点 " + location + "，预测次数：" + count);
                                    warningRepository.save(warning);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // 记录错误但不中断循环
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 四级预警规则：蓝→黄→橙→红，并设置推送对象（课题要求）
     * 蓝：小违章，推路段网格员；黄：推网格员+片区；橙：推交警中队；红：大事故，推交警支队+消防
     */
    private void applyLevelAndPushTarget(Warning w, String severity, int count) {
        String level;
        String pushTarget;
        if ("High".equals(severity) || count >= 10) {
            level = "RED";
            pushTarget = "交警支队、消防";
        } else if (count >= 5) {
            level = "ORANGE";
            pushTarget = "交警中队";
        } else if (count >= 3) {
            level = "YELLOW";
            pushTarget = "路段网格员、片区负责人";
        } else {
            level = "BLUE";
            pushTarget = "路段网格员";
        }
        w.setLevel(level);
        w.setPushTarget(pushTarget);
    }

    /**
     * 基于天气因素的智能预警（示例规则）：
     * - 遍历若干典型恶劣天气（大雨/大雾/冰雪等）
     * - 对高风险区域生成附加 Warning，提醒减速慢行、保持车距等
     *
     * 说明：这里使用简单规则模拟，可在论文中说明为“规则引擎 + 气象数据融合”的 demo 实现。
     */
    public List<Warning> generateWeatherWarningsOnce() {
        List<Warning> warnings = new ArrayList<>();
        Random random = new Random();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (String weather : WEATHER_TYPES) {
            for (String loc : WEATHER_LOCATIONS) {
                // 为了避免生成过多记录，随机跳过一部分组合
                if (random.nextBoolean()) {
                    continue;
                }

                int base = ("暴雨".equals(weather) || "冰雪路面".equals(weather)) ? 8 : 4;
                int fluctuation = random.nextInt(4); // 0-3
                int count = base + fluctuation;

                Warning w = new Warning();
                w.setWarningType("Weather");
                w.setLocation(loc);
                w.setViolationType("雨天/低能见度易发违章");
                w.setCount(count);
                w.setWeatherFactor(weather);

                String severity = (count >= 8) ? "High" : "Medium";
                w.setSeverity(severity);
                applyLevelAndPushTarget(w, severity, count);

                String district = loc;
                int idx = loc.indexOf("-");
                if (idx > 0) {
                    district = loc.substring(0, idx);
                }

                String msg = String.format(
                        "天气预警：%s，%s 受「%s」影响，预计违章风险上升（预测次数约 %d 起），建议减速慢行、保持车距。",
                        fmt.format(new Date()),
                        district,
                        weather,
                        count
                );
                w.setMessage(msg);

                warningRepository.save(w);
                warnings.add(w);
            }
        }

        return warnings;
    }

    /**
     * 定时任务：每天早上 6:30 根据天气规则生成一批智能预警
     * （用于系统自动运行场景，前端 latest-warnings 会自动展示）
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 30 6 * * ?")
    public void generateWeatherWarningsScheduled() {
        generateWeatherWarningsOnce();
    }
}
