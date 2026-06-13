package com.zhihui.bishe.dto;

import java.sql.Timestamp;

public class WarningDTO {
    private String message;
    private Timestamp warningTime;
    private Long violationId;
    private Long userId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Timestamp getWarningTime() { return warningTime; }
    public void setWarningTime(Timestamp warningTime) { this.warningTime = warningTime; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
