package com.zhihui.bishe.service;

import com.zhihui.bishe.model.Warning;
import com.zhihui.bishe.repository.WarningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WarningSimulationService {

    @Autowired
    private WarningRepository warningRepository;

    private static final List<String> LOCATIONS = Arrays.asList(
            "郑东新区-金水东路",
            "郑东新区-商务内环路",
            "金水区-花园路",
            "金水区-中州大道",
            "二七区-大学路",
            "二七区-京广路",
            "中原区-建设路",
            "中原区-嵩山路",
            "管城回族区-航海东路",
            "惠济区-文化北路"
    );

    private static final List<String> TYPES = Arrays.asList(
            "闯红灯", "超速行驶", "违法停车", "未系安全带", "违法变道", "占用应急车道"
    );

    private static class LevelMeta {
        String level;
        String pushTarget;
        String severity;
        int countBase;

        LevelMeta(String level, String pushTarget, String severity, int countBase) {
            this.level = level;
            this.pushTarget = pushTarget;
            this.severity = severity;
            this.countBase = countBase;
        }
    }

    private static final List<LevelMeta> LEVELS = Arrays.asList(
            new LevelMeta("BLUE", "路段网格员", "Low", 1),
            new LevelMeta("YELLOW", "路段网格员、片区负责人", "Medium", 3),
            new LevelMeta("ORANGE", "交警中队", "Medium", 6),
            new LevelMeta("RED", "交警支队、消防", "High", 12)
    );

    /** 生成演示预警：蓝/黄/橙/红各 perLevel 条 */
    public List<Warning> generateDemoWarnings(int perLevel) {
        int n = Math.max(1, Math.min(perLevel, 50));
        Random r = new Random();
        List<Warning> created = new ArrayList<>();

        for (LevelMeta meta : LEVELS) {
            for (int i = 0; i < n; i++) {
                Warning w = new Warning();
                w.setWarningType("DEMO");
                w.setLevel(meta.level);
                w.setPushTarget(meta.pushTarget);
                w.setSeverity(meta.severity);

                String location = LOCATIONS.get(r.nextInt(LOCATIONS.size()));
                String type = TYPES.get(r.nextInt(TYPES.size()));
                int count = meta.countBase + r.nextInt(Math.max(1, meta.countBase));
                w.setLocation(location);
                w.setViolationType(type);
                w.setCount(count);

                int hour = r.nextInt(24);
                w.setMessage("预警：时间：" + hour + "时，类型：" + type + "，地点：" + location + "，预测次数：" + count);

                created.add(warningRepository.save(w));
            }
        }

        return created;
    }
}

