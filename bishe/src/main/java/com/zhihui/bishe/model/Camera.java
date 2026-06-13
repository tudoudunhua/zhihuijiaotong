package com.zhihui.bishe.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "camera_device")
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "camera_id")
    private Long cameraId;

    @Column(name = "camera_name", nullable = false)
    private String cameraName;

    /**
     * 摄像头物理安装位置，建议和违章位置中的区域/路口保持一致
     */
    @Column(name = "location")
    private String location;

    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * ONLINE / OFFLINE / FAULT
     */
    @Column(name = "status")
    private String status;

    @Column(name = "install_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date installTime;

    @Column(name = "last_heartbeat")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastHeartbeat;

    @Column(name = "remark")
    private String remark;

    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getInstallTime() {
        return installTime;
    }

    public void setInstallTime(Date installTime) {
        this.installTime = installTime;
    }

    public Date getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Date lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}


