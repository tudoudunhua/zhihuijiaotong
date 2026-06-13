package com.zhihui.bishe.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zhihui.bishe.config.DateDeserializer;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "traffic_violation")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "violation_type")
    private String violationType;

    @Column(name = "violation_time")
    @JsonDeserialize(using = DateDeserializer.class)
    private Date violationTime;

    // fineAmount 仍然是 Double 类型
    @Column(name = "fine_amount")
    private Double fineAmount;

    @Column(name = "points_deducted")
    private Integer pointsDeducted;
    
    @Column(name = "road_id")
    private Long roadId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "violation_location")
    private String violationLocation;

    @Column(name = "violation_img")
    private String violationImg;

    @Column(name = "warning_level")
    private String warningLevel;

    @Column(name = "dispose_status")
    private Integer disposeStatus;

    @Column(name = "dispose_time")
    private Date disposeTime;

    @Column(name = "dispose_user")
    private Long disposeUser;

    @Column(name = "create_time")
    private Date createTime;

    // Getter 和 Setter 方法
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
    public Long getViolationId() {
        return violationId;
    }

    public void setViolationId(Long violationId) {
        this.violationId = violationId;
    }

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
    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Violation)) return false;
        Violation violation = (Violation) o;
        return Objects.equals(violationId, violation.violationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(violationId);
    }
}
