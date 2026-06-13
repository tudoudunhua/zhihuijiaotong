package com.zhihui.bishe.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "car_owner")
public class CarOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "plate_number", unique = true)
    private String plateNumber;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "phone_number")
    private String phoneNumber;

    /** 是否允许短信通知（默认 true） */
    @Column(name = "allow_sms")
    private Boolean allowSms;

    /** 是否允许电话通知（默认 true） */
    @Column(name = "allow_phone")
    private Boolean allowPhone;

    /** 是否允许站内留言/通知（默认 true） */
    @Column(name = "allow_message")
    private Boolean allowMessage;

    /**
     * 偏好渠道：AUTO / SMS / PHONE / MESSAGE
     * 不强制使用；AUTO 时按系统策略自动选择
     */
    @Column(name = "preferred_channel")
    private String preferredChannel;

    @Column(name = "update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getAllowSms() {
        return allowSms;
    }

    public void setAllowSms(Boolean allowSms) {
        this.allowSms = allowSms;
    }

    public Boolean getAllowPhone() {
        return allowPhone;
    }

    public void setAllowPhone(Boolean allowPhone) {
        this.allowPhone = allowPhone;
    }

    public Boolean getAllowMessage() {
        return allowMessage;
    }

    public void setAllowMessage(Boolean allowMessage) {
        this.allowMessage = allowMessage;
    }

    public String getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(String preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

