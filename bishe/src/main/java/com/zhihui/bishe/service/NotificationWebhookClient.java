package com.zhihui.bishe.service;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationWebhookClient {

    private final RestTemplate restTemplate;

    public NotificationWebhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WebhookResult send(String webhookUrl,
                              String secret,
                              String channel,
                              String to,
                              String content,
                              Long violationId,
                              String plateNumber) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return WebhookResult.failed("WEBHOOK_URL_EMPTY", "webhookUrl 未配置");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("to", to);
        payload.put("content", content);
        payload.put("violationId", violationId);
        payload.put("plateNumber", plateNumber);
        payload.put("timestamp", new Date());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (secret != null && !secret.trim().isEmpty()) {
            headers.set("X-Notify-Secret", secret.trim());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(webhookUrl.trim(), HttpMethod.POST, entity, String.class);
            String body = resp.getBody();
            String responseText = "HTTP " + resp.getStatusCodeValue() + (body != null ? (": " + truncate(body, 1800)) : "");
            if (resp.getStatusCode().is2xxSuccessful()) {
                return WebhookResult.ok(responseText);
            }
            return WebhookResult.failed("HTTP_" + resp.getStatusCodeValue(), responseText);
        } catch (RestClientException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return WebhookResult.failed("REQUEST_ERROR", truncate(msg, 400));
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    public static class WebhookResult {
        private boolean ok;
        private String code;
        private String message;

        public static WebhookResult ok(String message) {
            WebhookResult r = new WebhookResult();
            r.ok = true;
            r.code = "OK";
            r.message = message;
            return r;
        }

        public static WebhookResult failed(String code, String message) {
            WebhookResult r = new WebhookResult();
            r.ok = false;
            r.code = code;
            r.message = message;
            return r;
        }

        public boolean isOk() {
            return ok;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

