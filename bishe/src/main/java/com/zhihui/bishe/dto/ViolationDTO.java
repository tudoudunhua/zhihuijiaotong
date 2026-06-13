package com.zhihui.bishe.dto;

import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.util.PrivacyUtil;

import java.util.Date;

public class ViolationDTO {
    private Long violationId;
    private String plateNumber;
    private String violationType;
    private Date violationTime;
    private Double fineAmount;
    private Integer pointsDeducted;
    private Long roadId;
    private Long deviceId;
    private String violationLocation;
    private String violationImg;
    private String warningLevel;
    private Integer disposeStatus;
    private Date disposeTime;
    private Long disposeUser;
    private Date createTime;

    // Getters and Setters
    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public Date getViolationTime() {
        return violationTime;
    }

    public void setViolationTime(Date violationTime) {
        this.violationTime = violationTime;
    }

    public Double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(Double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public Integer getPointsDeducted() {
        return pointsDeducted;
    }

    public void setPointsDeducted(Integer pointsDeducted) {
        this.pointsDeducted = pointsDeducted;
    }

    public Long getRoadId() {
        return roadId;
    }

    public void setRoadId(Long roadId) {
        this.roadId = roadId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getViolationLocation() {
        return violationLocation;
    }

    public void setViolationLocation(String violationLocation) {
        this.violationLocation = violationLocation;
    }

    public String getViolationImg() {
        return violationImg;
    }

    public void setViolationImg(String violationImg) {
        this.violationImg = violationImg;
    }

    public String getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(String warningLevel) {
        this.warningLevel = warningLevel;
    }

    public Integer getDisposeStatus() {
        return disposeStatus;
    }

    public void setDisposeStatus(Integer disposeStatus) {
        this.disposeStatus = disposeStatus;
    }

    public Date getDisposeTime() {
        return disposeTime;
    }

    public void setDisposeTime(Date disposeTime) {
        this.disposeTime = disposeTime;
    }

    public Long getDisposeUser() {
        return disposeUser;
    }

    public void setDisposeUser(Long disposeUser) {
        this.disposeUser = disposeUser;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getViolationId() {
        return violationId;
    }

    public void setViolationId(Long violationId) {
        this.violationId = violationId;
    }

    /** 从实体构建 DTO，maskPlate 为 true 时对车牌脱敏（非管理员） */
    public static ViolationDTO from(Violation v, boolean maskPlate) {
        ViolationDTO dto = new ViolationDTO();
        dto.setViolationId(v.getViolationId());
        dto.setPlateNumber(maskPlate ? PrivacyUtil.maskPlateNumber(v.getPlateNumber()) : v.getPlateNumber());
        dto.setViolationType(v.getViolationType());
        dto.setViolationTime(v.getViolationTime());
        dto.setFineAmount(v.getFineAmount());
        dto.setPointsDeducted(v.getPointsDeducted());
        dto.setRoadId(v.getRoadId());
        dto.setDeviceId(v.getDeviceId());
        dto.setViolationLocation(v.getViolationLocation());
        dto.setViolationImg(v.getViolationImg());
        dto.setWarningLevel(v.getWarningLevel());
        dto.setDisposeStatus(v.getDisposeStatus());
        dto.setDisposeTime(v.getDisposeTime());
        dto.setDisposeUser(v.getDisposeUser());
        dto.setCreateTime(v.getCreateTime());
        return dto;
    }
}
