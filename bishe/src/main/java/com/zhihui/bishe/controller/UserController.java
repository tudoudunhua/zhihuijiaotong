package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.User;
import com.zhihui.bishe.repository.UserRepository;
import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.util.RoleUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("仅管理员可查看用户管理");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole());
            m.put("menuPermissions", splitPermissions(u.getMenuPermissions()));
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<?> updatePermissions(@PathVariable Long id,
                                               @RequestBody UpdatePermissionRequest body,
                                               HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("仅管理员可修改用户权限");
        }
        Optional<User> optional = userRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }
        User user = optional.get();
        if (body.getRole() != null && !body.getRole().trim().isEmpty()) {
            user.setRole(RoleUtil.normalize(body.getRole()));
        }
        List<String> permissions = body.getMenuPermissions() == null ? Collections.emptyList() : body.getMenuPermissions();
        String merged = String.join(",", permissions.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new));
        user.setMenuPermissions(merged);
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "权限更新成功");
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest body, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("仅管理员可新增用户");
        }
        String username = body.getUsername() == null ? "" : body.getUsername().trim();
        String password = body.getPassword() == null ? "" : body.getPassword().trim();
        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("用户名和密码不能为空");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("用户名已存在");
        }

        String role = body.getRole() == null || body.getRole().trim().isEmpty()
                ? "USER"
                : RoleUtil.normalize(body.getRole());
        List<String> permissions = body.getMenuPermissions();
        if (permissions == null || permissions.isEmpty()) {
            permissions = defaultPermissionsByRole(role);
        }
        String merged = String.join(",", permissions.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new));

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(body.getRealName() == null ? username : body.getRealName().trim());
        user.setRole(role);
        user.setMenuPermissions(merged);
        user.setCreateTime(new Date());
        userRepository.save(user);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "用户创建成功");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("仅管理员可删除用户");
        }
        Optional<User> optional = userRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }
        User user = optional.get();
        String currentUsername = JwtUtil.getUsernameFromAuthorization(request.getHeader("Authorization"));
        if (currentUsername != null && currentUsername.equalsIgnoreCase(user.getUsername())) {
            return ResponseEntity.badRequest().body("不能删除当前登录用户");
        }

        if (RoleUtil.isAdmin(user.getRole())) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> RoleUtil.isAdmin(u.getRole()))
                    .count();
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body("系统至少需要保留一个管理员");
            }
        }

        userRepository.deleteById(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "用户删除成功");
        return ResponseEntity.ok(res);
    }

    private boolean isAdmin(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return RoleUtil.isAdmin(JwtUtil.getRoleFromToken(auth));
    }

    private List<String> splitPermissions(String permissionCsv) {
        if (permissionCsv == null || permissionCsv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] arr = permissionCsv.split(",");
        List<String> list = new ArrayList<>();
        for (String a : arr) {
            String s = a.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    private List<String> defaultPermissionsByRole(String role) {
        String r = RoleUtil.normalize(role);
        if ("ADMIN".equals(r)) {
            return Arrays.asList("view_home", "view_violations", "view_pending", "view_add_violation", "view_statistics", "view_intelligent", "view_warnings", "view_cameras", "view_user_manage", "view_operation_logs");
        }
        if ("OFFICER".equals(r)) {
            return Arrays.asList("view_home", "view_violations", "view_pending", "view_add_violation", "view_warnings", "view_cameras");
        }
        if ("ANALYST".equals(r)) {
            return Arrays.asList("view_home", "view_statistics", "view_intelligent", "view_warnings");
        }
        if ("ROAD_GRID".equals(r) || "GRID_MEMBER".equals(r) || "TRAFFIC_SQUAD".equals(r) || "FIRE".equals(r)) {
            return Arrays.asList("view_home", "view_warnings");
        }
        return Arrays.asList("view_home", "view_violations");
    }

    public static class UpdatePermissionRequest {
        private String role;
        private List<String> menuPermissions;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<String> getMenuPermissions() {
            return menuPermissions;
        }

        public void setMenuPermissions(List<String> menuPermissions) {
            this.menuPermissions = menuPermissions;
        }
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private String role;
        private List<String> menuPermissions;

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

        public List<String> getMenuPermissions() {
            return menuPermissions;
        }

        public void setMenuPermissions(List<String> menuPermissions) {
            this.menuPermissions = menuPermissions;
        }
    }
}
