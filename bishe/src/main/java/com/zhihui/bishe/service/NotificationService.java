package com.zhihui.bishe.service;

import com.zhihui.bishe.model.CarOwner;
import com.zhihui.bishe.model.NotificationLog;
import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.repository.CarOwnerRepository;
import com.zhihui.bishe.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private CarOwnerRepository carOwnerRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationWebhookClient webhookClient;

    /** SIMULATE / WEBHOOK */
    @Value("${notification.provider-mode:SIMULATE}")
    private String providerMode;

    /** WEBHOOK 模式下的 URL */
    @Value("${notification.webhook.url:}")
    private String webhookUrl;

    /** WEBHOOK 模式下用于鉴权的共享密钥（可空） */
    @Value("${notification.webhook.secret:}")
    private String webhookSecret;

    public NotificationLog notifyOwner(Violation violation,
                                       String requestedChannel,
                                       String requestedContent,
                                       Long operatorUserId,
                                       String operatorUsername) {

        NotificationLog log = new NotificationLog();
        log.setViolationId(violation != null ? violation.getViolationId() : null);
        log.setPlateNumber(violation != null ? violation.getPlateNumber() : null);
        log.setCreatedTime(new Date());
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUsername(operatorUsername);

        if (violation == null) {
            log.setStatus("FAILED");
            log.setChannel(normalizeChannel(requestedChannel));
            log.setContent("通知失败：违章记录不存在");
            return notificationLogRepository.save(log);
        }

        String plate = violation.getPlateNumber();
        Optional<CarOwner> optOwner = (plate == null || plate.trim().isEmpty())
                ? Optional.empty()
                : carOwnerRepository.findByPlateNumber(plate.trim());
        CarOwner owner = optOwner.orElse(null);

        if (owner != null) {
            log.setOwnerId(owner.getOwnerId());
        }

        String channel = chooseChannel(requestedChannel, owner);
        log.setChannel(channel);

        String content = (requestedContent != null && !requestedContent.trim().isEmpty())
                ? requestedContent.trim()
                : buildDefaultContent(violation, owner);

        if (channel == null) {
            log.setStatus("FAILED");
            log.setProviderMode(getProviderModeNormalized());
            log.setErrorMessage("NO_AVAILABLE_CHANNEL");
            log.setContent("通知失败：未找到可用通知方式（请先录入车主手机号或开启留言）");
            return notificationLogRepository.save(log);
        }

        SendResult r = sendWithMode(channel, owner, content, violation);
        log.setProviderMode(getProviderModeNormalized());
        log.setProviderResponse(r != null ? r.providerResponse : null);
        log.setErrorMessage(r != null ? r.errorMessage : null);
        log.setStatus(r != null && r.ok ? "SENT" : "FAILED");
        log.setContent(content);
        return notificationLogRepository.save(log);
    }

    private String getProviderModeNormalized() {
        if (providerMode == null) return "SIMULATE";
        String m = providerMode.trim().toUpperCase(Locale.ROOT);
        return "WEBHOOK".equals(m) ? "WEBHOOK" : "SIMULATE";
    }

    private String normalizeChannel(String channel) {
        if (channel == null) return null;
        String c = channel.trim().toUpperCase(Locale.ROOT);
        if (c.isEmpty() || "AUTO".equals(c)) return "AUTO";
        if ("SMS".equals(c) || "PHONE".equals(c) || "MESSAGE".equals(c)) return c;
        return "AUTO";
    }

    private String chooseChannel(String requestedChannel, CarOwner owner) {
        String req = normalizeChannel(requestedChannel);
        if (req != null && !"AUTO".equals(req)) {
            return isChannelAvailable(req, owner) ? req : null;
        }

        // AUTO：先看偏好，再按可用性降级
        if (owner != null) {
            String pref = normalizeChannel(owner.getPreferredChannel());
            if (pref != null && !"AUTO".equals(pref) && isChannelAvailable(pref, owner)) {
                return pref;
            }
        }

        // 默认策略：短信 > 电话 > 留言
        if (isChannelAvailable("SMS", owner)) return "SMS";
        if (isChannelAvailable("PHONE", owner)) return "PHONE";
        if (isChannelAvailable("MESSAGE", owner)) return "MESSAGE";
        return null;
    }

    private boolean isChannelAvailable(String channel, CarOwner owner) {
        if (owner == null) {
            // 无车主信息时，只能留言（但也需要允许留言；默认认为不允许）
            return false;
        }

        String phone = owner.getPhoneNumber() != null ? owner.getPhoneNumber().trim() : "";
        boolean hasPhone = !phone.isEmpty();

        if ("SMS".equals(channel)) {
            return hasPhone && (owner.getAllowSms() == null || Boolean.TRUE.equals(owner.getAllowSms()));
        }
        if ("PHONE".equals(channel)) {
            return hasPhone && (owner.getAllowPhone() == null || Boolean.TRUE.equals(owner.getAllowPhone()));
        }
        if ("MESSAGE".equals(channel)) {
            return owner.getAllowMessage() == null || Boolean.TRUE.equals(owner.getAllowMessage());
        }
        return false;
    }

    private String buildDefaultContent(Violation v, CarOwner owner) {
        String ownerName = owner != null ? owner.getOwnerName() : null;
        if (ownerName == null || ownerName.trim().isEmpty()) ownerName = "车主";

        String type = v.getViolationType() != null ? v.getViolationType() : "违章";
        String loc = v.getViolationLocation() != null ? v.getViolationLocation() : "未知地点";
        String time = v.getViolationTime() != null ? v.getViolationTime().toString() : "未知时间";

        return "【智慧交通系统】" + ownerName + "您好，您的车辆（" + v.getPlateNumber()
                + "）在 " + time + " 于 " + loc + " 发生" + type
                + "，请尽快登录系统/到交管窗口完成处理。";
    }

    /**
     * 发送：
     * - SIMULATE：本地模拟成功/失败
     * - WEBHOOK：对接你自己的短信/语音/留言平台（HTTP POST）
     */
    private SendResult sendWithMode(String channel, CarOwner owner, String content, Violation violation) {
        if (channel == null) return SendResult.failed("CHANNEL_NULL", null);

        String mode = getProviderModeNormalized();
        if ("WEBHOOK".equals(mode)) {
            String to = null;
            if ("SMS".equals(channel) || "PHONE".equals(channel)) {
                to = owner != null && owner.getPhoneNumber() != null ? owner.getPhoneNumber().trim() : null;
            } else if ("MESSAGE".equals(channel)) {
                // 留言也允许没有手机号，to 可为空
                to = owner != null ? String.valueOf(owner.getOwnerId()) : null;
            }

            NotificationWebhookClient.WebhookResult r = webhookClient.send(
                    webhookUrl,
                    webhookSecret,
                    channel,
                    to,
                    content,
                    violation != null ? violation.getViolationId() : null,
                    violation != null ? violation.getPlateNumber() : null
            );
            if (r != null && r.isOk()) {
                return SendResult.ok(r.getMessage());
            }
            return SendResult.failed(r != null ? r.getCode() : "WEBHOOK_NULL", r != null ? r.getMessage() : null);
        }

        // SIMULATE
        if ("SMS".equals(channel) || "PHONE".equals(channel)) {
            String phone = owner != null && owner.getPhoneNumber() != null ? owner.getPhoneNumber().trim() : "";
            return !phone.isEmpty() ? SendResult.ok("SIMULATE_OK") : SendResult.failed("NO_PHONE", "手机号为空");
        }
        if ("MESSAGE".equals(channel)) return SendResult.ok("SIMULATE_OK");
        return SendResult.failed("UNSUPPORTED_CHANNEL", channel);
    }

    private static class SendResult {
        private boolean ok;
        private String errorMessage;
        private String providerResponse;

        static SendResult ok(String providerResponse) {
            SendResult r = new SendResult();
            r.ok = true;
            r.providerResponse = providerResponse;
            return r;
        }

        static SendResult failed(String errorMessage, String providerResponse) {
            SendResult r = new SendResult();
            r.ok = false;
            r.errorMessage = errorMessage;
            r.providerResponse = providerResponse;
            return r;
        }
    }
}

