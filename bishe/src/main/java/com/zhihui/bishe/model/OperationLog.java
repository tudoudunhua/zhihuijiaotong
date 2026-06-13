package com.zhihui.bishe.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "operation_log", indexes = {
        @Index(name = "idx_op_log_time", columnList = "created_time"),
        @Index(name = "idx_op_log_user", columnList = "username")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "role", length = 32)
    private String role;

    /** 模块：AUTH / USER / VIOLATION / CAMERA / WARNING / SIMULATE / OWNER / SYSTEM */
    @Column(name = "module", length = 32)
    private String module;

    @Column(name = "action", length = 128)
    private String action;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "request_uri", length = 512)
    private String requestUri;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /** SUCCESS / FAIL */
    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "created_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
