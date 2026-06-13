package com.zhihui.bishe.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")   // ⭐ 关键
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "real_name") // ⭐ 关键
    private String realName;

    @Column(name = "role")
    private String role;        // ⭐ 数据库是 varchar，不能用 Integer

    @Column(name = "menu_permissions", length = 1000)
    private String menuPermissions;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    // ===== Getter / Setter =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMenuPermissions() {
        return menuPermissions;
    }

    public void setMenuPermissions(String menuPermissions) {
        this.menuPermissions = menuPermissions;
    }
}
