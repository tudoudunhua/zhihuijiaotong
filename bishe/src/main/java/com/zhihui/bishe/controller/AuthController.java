package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.User;
import com.zhihui.bishe.repository.UserRepository;
import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.util.RoleUtil;
import com.zhihui.bishe.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Resource
    private UserRepository userRepository;

    @Resource
    private OperationLogService operationLogService;

    /**
     * 登录接口（数据库校验 + JWT）
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        Map<String, Object> result = new HashMap<>();
        String ip = resolveClientIp(httpRequest);
        String username = request != null ? request.getUsername() : null;

        // 1️⃣ 参数校验
        if (request.getUsername() == null || request.getPassword() == null) {
            result.put("success", false);
            result.put("message", "用户名或密码不能为空");
            operationLogService.recordLogin(username, false, "用户名或密码不能为空", ip, null);
            return result;
        }

        // 2️⃣ 查询用户
        Optional<User> optionalUser =
                userRepository.findByUsername(request.getUsername());

        if (!optionalUser.isPresent()) {
            result.put("success", false);
            result.put("message", "用户名不存在");
            operationLogService.recordLogin(username, false, "用户名不存在", ip, null);
            return result;
        }

        User user = optionalUser.get();

        // 3️⃣ 校验密码（你现在是明文，毕业设计可接受）
        if (!request.getPassword().equals(user.getPassword())) {
            result.put("success", false);
            result.put("message", "密码错误");
            operationLogService.recordLogin(username, false, "密码错误", ip, user);
            return result;
        }

        // 4️⃣ 生成 JWT（含 role，用于权限与车牌脱敏）
        String token = JwtUtil.generateToken(user.getUsername(), user.getRole());

        // 5️⃣ 返回结果
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", RoleUtil.normalize(user.getRole()));
        userInfo.put("menuPermissions", user.getMenuPermissions());

        result.put("user", userInfo);

        operationLogService.recordLogin(user.getUsername(), true, "登录成功", ip, user);
        return result;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.trim().isEmpty()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.trim().isEmpty()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 登录请求体
     */
    public static class LoginRequest {
        private String username;
        private String password;

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
    }
}
