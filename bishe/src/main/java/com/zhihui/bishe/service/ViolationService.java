package com.zhihui.bishe.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.repository.ViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ViolationService {

    @Autowired
    private ViolationRepository violationRepository;

    // ========== 阿里云 OSS 配置 ==========
    @Value("${aliyun.oss.endpoint}")
    private String ossEndpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String ossAccessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String ossAccessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String ossBucketName;

    @Value("${aliyun.oss.base-url}")
    private String ossBaseUrl;

    @Value("${aliyun.oss.violation-folder:violations}")
    private String violationFolder;

    /**
     * 查询全部违章
     */
    public List<Violation> getAllViolations() {
        return violationRepository.findAll();
    }

    /** 查询待处理违章（disposeStatus 为空或 0） */
    public List<Violation> getPendingViolations() {
        return violationRepository.findPendingOrderByViolationTimeDesc();
    }

    /**
     * 根据ID查询违章
     */
    public Violation getViolationById(Long id) {
        Optional<Violation> optional = violationRepository.findById(id);
        return optional.orElse(null);
    }

    /** 标记已处理 */
    public Violation markProcessed(Long id, Long disposeUserId) {
        Optional<Violation> optional = violationRepository.findById(id);
        if (!optional.isPresent()) return null;
        Violation v = optional.get();
        v.setDisposeStatus(1);
        v.setDisposeTime(new Date());
        v.setDisposeUser(disposeUserId);
        return violationRepository.save(v);
    }

    /**
     * 新增违章
     */
    public Violation addViolation(Violation violation) {
        if (isBlank(violation.getViolationImg())) {
            violation.setViolationImg(buildDemoImageUrlByType(violation.getViolationType()));
        }
        return violationRepository.save(violation);
    }

    /**
     * 更新违章
     */
    public Violation updateViolation(Long id, Violation violation) {
        Optional<Violation> optional = violationRepository.findById(id);
        if (!optional.isPresent()) {
            return null;
        }

        Violation existing = optional.get();

        existing.setPlateNumber(violation.getPlateNumber());
        existing.setViolationType(violation.getViolationType());
        existing.setViolationTime(violation.getViolationTime());
        existing.setFineAmount(violation.getFineAmount());
        existing.setPointsDeducted(violation.getPointsDeducted());
        existing.setViolationLocation(violation.getViolationLocation());
        String incomingImg = violation.getViolationImg();
        if (!isBlank(incomingImg)) {
            // 前端明确传了图片（上传后的 OSS URL），按前端值更新
            existing.setViolationImg(incomingImg);
        } else {
            // 未上传图片时，按违章类型自动匹配示例图，避免图片为空
            existing.setViolationImg(buildDemoImageUrlByType(violation.getViolationType()));
        }
        existing.setRoadId(violation.getRoadId());
        existing.setDeviceId(violation.getDeviceId());
        existing.setDisposeStatus(violation.getDisposeStatus());
        existing.setDisposeTime(violation.getDisposeTime());
        existing.setDisposeUser(violation.getDisposeUser());

        return violationRepository.save(existing);
    }

    /**
     * 删除违章
     */
    public boolean deleteViolation(Long id) {
        if (!violationRepository.existsById(id)) {
            return false;
        }
        violationRepository.deleteById(id);
        return true;
    }

    /**
     * 保存 / 更新违章照片到阿里云 OSS，并返回可访问的 URL 路径
     */
    public String saveViolationImage(Long id, MultipartFile file) throws IOException {
        Optional<Violation> optional = violationRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Violation not found, id=" + id);
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') != -1) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = "violation-" + id + "-" + System.currentTimeMillis() + ext;

        // OSS 对象键：例如 violations/violation-1-xxx.jpg
        String objectKey = violationFolder.endsWith("/")
                ? violationFolder + filename
                : violationFolder + "/" + filename;

        // 上传到阿里云 OSS
        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
        try {
            ossClient.putObject(ossBucketName, objectKey, file.getInputStream());
        } finally {
            ossClient.shutdown();
        }

        // 生成可直接访问的 URL（使用配置的 base-url）
        String base = ossBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String urlPath = base + "/" + objectKey;

        Violation violation = optional.get();
        violation.setViolationImg(urlPath);
        violationRepository.save(violation);

        return urlPath;
    }

    /**
     * 为指定违章记录生成一个“示例违章照片”地址（不实际上传文件），
     * 根据违章类型选择不同的示例图片，直接写入 violationImg。
     * 前提：你在 OSS 的 demo/ 目录下提前放好对应文件。
     */
    public String generateDemoViolationImage(Long id) {
        Optional<Violation> optional = violationRepository.findById(id);
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Violation not found, id=" + id);
        }
        Violation violation = optional.get();
        String urlPath = buildDemoImageUrlByType(violation.getViolationType());

        violation.setViolationImg(urlPath);
        violationRepository.save(violation);
        return urlPath;
    }

    private String buildDemoImageUrlByType(String violationType) {
        String type = violationType == null ? "" : violationType;
        String code;
        if (type.contains("闯红灯")) {
            code = "red_light.jpg";
        } else if (type.contains("超速")) {
            code = "speeding.jpg";
        } else if (type.contains("违法停车")) {
            code = "parking.jpg";
        } else if (type.contains("未系安全带")) {
            code = "seatbelt.jpg";
        } else if (type.contains("酒驾")) {
            code = "drunk.jpg";
        } else if (type.contains("逆行")) {
            code = "reverse.jpg";
        } else if (type.contains("违法变道")) {
            code = "lane_change.jpg";
        } else if (type.contains("占用应急车道")) {
            code = "emergency_lane.jpg";
        } else if (type.contains("无证驾驶")) {
            code = "unlicensed.jpg";
        } else {
            code = "other.jpg";
        }

        String base = ossBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/demo/" + code;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}


