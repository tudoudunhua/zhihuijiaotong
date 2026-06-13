package com.zhihui.bishe.controller;

import com.zhihui.bishe.dto.ViolationDTO;
import com.zhihui.bishe.model.NotificationLog;
import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.util.RoleUtil;
import com.zhihui.bishe.service.NotificationService;
import com.zhihui.bishe.service.ViolationService;
import com.zhihui.bishe.repository.UserRepository;
import com.zhihui.bishe.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/violations")
@CrossOrigin   // 这里保留没问题，但真正生效的是 Security 里的 CORS
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /** 查询全部违章（非管理员返回车牌脱敏） */
    @GetMapping
    public List<ViolationDTO> getAllViolations(HttpServletRequest request) {
        boolean maskPlate = needMaskPlate(request);
        return violationService.getAllViolations().stream()
                .map(v -> ViolationDTO.from(v, maskPlate))
                .collect(Collectors.toList());
    }

    /** 查询待处理违章（disposeStatus 为空或 0；非管理员返回车牌脱敏） */
    @GetMapping("/pending")
    public List<ViolationDTO> getPendingViolations(HttpServletRequest request) {
        boolean maskPlate = needMaskPlate(request);
        return violationService.getPendingViolations().stream()
                .map(v -> ViolationDTO.from(v, maskPlate))
                .collect(Collectors.toList());
    }

    /** 根据 ID 查询（非管理员返回车牌脱敏） */
    @GetMapping("/{id}")
    public ResponseEntity<ViolationDTO> getViolationById(@PathVariable Long id, HttpServletRequest request) {
        Violation violation = violationService.getViolationById(id);
        if (violation == null) return ResponseEntity.notFound().build();
        boolean maskPlate = needMaskPlate(request);
        return ResponseEntity.ok(ViolationDTO.from(violation, maskPlate));
    }

    private boolean needMaskPlate(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return !RoleUtil.isAdmin(JwtUtil.getRoleFromToken(auth));
    }

    private Long getOperatorUserId(HttpServletRequest request) {
        try {
            String username = JwtUtil.getUsernameFromAuthorization(request.getHeader("Authorization"));
            if (username == null || username.trim().isEmpty()) return null;
            return userRepository.findByUsername(username.trim())
                    .map(User::getUserId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getOperatorUsername(HttpServletRequest request) {
        String username = JwtUtil.getUsernameFromAuthorization(request.getHeader("Authorization"));
        return username != null ? username : null;
    }

    /** 新增违章（含数据清洗：金额、扣分合理性校验） */
    @PostMapping
    public ResponseEntity<?> addViolation(@RequestBody Violation violation) {
        try {
            if (violation.getPlateNumber() == null || violation.getPlateNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("车牌号不能为空");
            }
            if (violation.getPlateNumber().length() > 10) {
                return ResponseEntity.badRequest().body("车牌号长度异常");
            }
            if (violation.getViolationType() == null || violation.getViolationType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("违章类型不能为空");
            }
            if (violation.getFineAmount() != null && (violation.getFineAmount() < 0 || violation.getFineAmount() > 10000)) {
                return ResponseEntity.badRequest().body("罚款金额需在 0～10000 元之间");
            }
            if (violation.getPointsDeducted() != null && (violation.getPointsDeducted() < 0 || violation.getPointsDeducted() > 12)) {
                return ResponseEntity.badRequest().body("扣分需在 0～12 分之间");
            }

            if (violation.getViolationTime() == null) {
                violation.setViolationTime(new Date());
            }
            if (violation.getDisposeStatus() == null) {
                // 摄像头自动抓拍默认未处理
                violation.setDisposeStatus(0);
            }
            if (violation.getCreateTime() == null) {
                violation.setCreateTime(new Date());
            }

            Violation saved = violationService.addViolation(violation);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("保存失败：" + e.getMessage());
        }
    }

    /** 标记违章已处理 */
    @PostMapping("/{id}/process")
    public ResponseEntity<?> processViolation(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long operatorUserId = getOperatorUserId(request);
            Violation v = violationService.markProcessed(id, operatorUserId);
            if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("违章记录不存在");
            boolean maskPlate = needMaskPlate(request);
            return ResponseEntity.ok(ViolationDTO.from(v, maskPlate));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("处理失败：" + e.getMessage());
        }
    }

    /** 通知车主（自动选择短信/电话/留言；可指定 channel 和 message） */
    @PostMapping("/{id}/notify")
    public ResponseEntity<?> notifyOwner(@PathVariable Long id,
                                         @RequestBody(required = false) NotifyRequest body,
                                         HttpServletRequest request) {
        try {
            Violation v = violationService.getViolationById(id);
            if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("违章记录不存在");

            String channel = body != null ? body.getChannel() : null;
            String message = body != null ? body.getMessage() : null;

            NotificationLog log = notificationService.notifyOwner(
                    v,
                    channel,
                    message,
                    getOperatorUserId(request),
                    getOperatorUsername(request)
            );
            return ResponseEntity.ok(new NotifyResponse(log));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("通知失败：" + e.getMessage());
        }
    }

    /** 更新（含数据清洗校验） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateViolation(
            @PathVariable Long id,
            @RequestBody Violation violation) {
        if (violation.getFineAmount() != null && (violation.getFineAmount() < 0 || violation.getFineAmount() > 10000)) {
            return ResponseEntity.badRequest().body("罚款金额需在 0～10000 元之间");
        }
        if (violation.getPointsDeducted() != null && (violation.getPointsDeducted() < 0 || violation.getPointsDeducted() > 12)) {
            return ResponseEntity.badRequest().body("扣分需在 0～12 分之间");
        }
        Violation updated = violationService.updateViolation(id, violation);
        return updated != null
                ? ResponseEntity.ok(updated)
                : ResponseEntity.notFound().build();
    }

    /** 通知请求 */
    public static class NotifyRequest {
        /** AUTO / SMS / PHONE / MESSAGE */
        private String channel;
        /** 自定义消息内容（可空） */
        private String message;

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /** 通知响应 */
    public static class NotifyResponse {
        private Long logId;
        private String channel;
        private String status;
        private String content;
        private String providerMode;
        private String providerResponse;
        private String errorMessage;
        private String createdTime;

        public NotifyResponse(NotificationLog log) {
            this.logId = log != null ? log.getLogId() : null;
            this.channel = log != null ? log.getChannel() : null;
            this.status = log != null ? log.getStatus() : null;
            this.content = log != null ? log.getContent() : null;
            this.providerMode = log != null ? log.getProviderMode() : null;
            this.providerResponse = log != null ? log.getProviderResponse() : null;
            this.errorMessage = log != null ? log.getErrorMessage() : null;
            this.createdTime = log != null && log.getCreatedTime() != null ? log.getCreatedTime().toString() : null;
        }

        public Long getLogId() {
            return logId;
        }

        public String getChannel() {
            return channel;
        }

        public String getStatus() {
            return status;
        }

        public String getContent() {
            return content;
        }

        public String getProviderMode() {
            return providerMode;
        }

        public String getProviderResponse() {
            return providerResponse;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getCreatedTime() {
            return createdTime;
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteViolation(@PathVariable Long id) {
        return violationService.deleteViolation(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * 上传 / 更换违章照片
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadViolationImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }
        try {
            String url = violationService.saveViolationImage(id, file);
            return ResponseEntity.ok(Collections.singletonMap("violationImg", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("保存图片失败：" + e.getMessage());
        }
    }

    /**
     * 为违章记录自动生成一张“示例违章照片”URL（不上传真实文件），
     * 方便演示和快速录入。
     */
    @PostMapping("/{id}/auto-image")
    public ResponseEntity<?> autoGenerateViolationImage(@PathVariable Long id) {
        try {
            String url = violationService.generateDemoViolationImage(id);
            return ResponseEntity.ok(Collections.singletonMap("violationImg", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("生成示例图片失败：" + e.getMessage());
        }
    }
}


