package com.zhihui.bishe.service;

import com.zhihui.bishe.model.Camera;
import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.repository.CameraRepository;
import com.zhihui.bishe.repository.ViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ViolationSimulationService {

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private CameraRepository cameraRepository;

    private static final List<String> TYPES = Arrays.asList(
            "超速行驶",
            "闯红灯",
            "违法停车",
            "未系安全带",
            "逆行",
            "违法变道",
            "占用应急车道"
    );

    private static final List<String> ZHENGZHOU_LOCATIONS = Arrays.asList(
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

    public List<Violation> generateCapturedViolations(int count) {
        int n = Math.max(1, Math.min(count, 200));
        Random r = new Random();
        List<Camera> cameras = cameraRepository.findAll();

        List<Violation> created = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String type = TYPES.get(r.nextInt(TYPES.size()));
            FinePoints fp = fineAndPoints(type, r);

            Camera cam = !cameras.isEmpty() ? cameras.get(r.nextInt(cameras.size())) : null;
            String location = cam != null && cam.getLocation() != null && !cam.getLocation().trim().isEmpty()
                    ? cam.getLocation().trim()
                    : ZHENGZHOU_LOCATIONS.get(r.nextInt(ZHENGZHOU_LOCATIONS.size()));

            Violation v = new Violation();
            v.setPlateNumber(randomPlate(r));
            v.setViolationType(type);
            v.setViolationLocation(location);
            v.setFineAmount(fp.fine);
            v.setPointsDeducted(fp.points);

            // 最近 0~48 小时内随机时间
            long now = System.currentTimeMillis();
            long offsetMs = (long) r.nextInt(48 * 60) * 60_000L;
            v.setViolationTime(new Date(now - offsetMs));

            // 关联摄像头
            if (cam != null) {
                v.setDeviceId(cam.getCameraId());
            }

            // 摄像头抓拍默认未处理
            v.setDisposeStatus(0);
            v.setCreateTime(new Date());

            // 简单给个预警等级（用于你现有表格展示）
            v.setWarningLevel(simpleWarningLevel(fp.points, fp.fine));

            created.add(violationRepository.save(v));
        }
        return created;
    }

    private String simpleWarningLevel(int points, double fine) {
        if (points >= 12 || fine >= 1000) return "RED";
        if (points >= 6 || fine >= 500) return "ORANGE";
        if (points >= 3 || fine >= 200) return "YELLOW";
        return "BLUE";
    }

    private FinePoints fineAndPoints(String type, Random r) {
        if ("闯红灯".equals(type)) return new FinePoints(200 + r.nextInt(200), 6);
        if ("超速行驶".equals(type)) return new FinePoints(100 + r.nextInt(400), 3 + r.nextInt(4));
        if ("违法停车".equals(type)) return new FinePoints(50 + r.nextInt(150), 0);
        if ("未系安全带".equals(type)) return new FinePoints(50 + r.nextInt(100), 1);
        if ("逆行".equals(type)) return new FinePoints(200 + r.nextInt(300), 3);
        if ("违法变道".equals(type)) return new FinePoints(100 + r.nextInt(200), 2);
        if ("占用应急车道".equals(type)) return new FinePoints(300 + r.nextInt(500), 6);
        return new FinePoints(100, 0);
    }

    private String randomPlate(Random r) {
        // 简化：豫A + 5位（数字/字母）
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        sb.append("豫A");
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static class FinePoints {
        double fine;
        int points;

        FinePoints(double fine, int points) {
            this.fine = fine;
            this.points = points;
        }
    }
}

