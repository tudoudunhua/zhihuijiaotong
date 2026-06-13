package com.zhihui.bishe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Warning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message; // 预警信息
    private Timestamp warningTime = new Timestamp(System.currentTimeMillis());

    private String warningType; // e.g., "PeakHour", "ML"
    private String severity;    // e.g., "High", "Medium", "Low"
    /** 四级预警：BLUE/YELLOW/ORANGE/RED，对应蓝/黄/橙/红 */
    private String level;
    /** 推送对象说明：如 路段网格员、交警支队+消防 */
    private String pushTarget;
    /** 额外的天气等风险因素说明，例如：大雨/大雾/冰雪路面 */
    private String weatherFactor;
    private String location;
    private String violationType;
    private Integer count;

    @ManyToOne
    @JoinColumn(name = "violation_id")
    @JsonIgnore
    private Violation violation;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // Getters and Setters
    public void setViolation(Violation violation) {
        this.violation = violation;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPushTarget() {
        return pushTarget;
    }

    public void setPushTarget(String pushTarget) {
        this.pushTarget = pushTarget;
    }

    public String getWeatherFactor() {
        return weatherFactor;
    }

    public void setWeatherFactor(String weatherFactor) {
        this.weatherFactor = weatherFactor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    
    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getWarningTime() {
        return warningTime;
    }
}
