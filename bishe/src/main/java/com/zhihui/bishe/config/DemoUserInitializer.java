package com.zhihui.bishe.config;

import com.zhihui.bishe.model.User;
import com.zhihui.bishe.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DemoUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    public DemoUserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureUser("admin", "123456", "系统管理员", "ADMIN", defaultPermissions("ADMIN"));
        ensureUser("officer", "123456", "交警处置员", "OFFICER", defaultPermissions("OFFICER"));
        ensureUser("analyst", "123456", "数据分析员", "ANALYST", defaultPermissions("ANALYST"));
        ensureUser("roadgrid", "123456", "路段网格员", "ROAD_GRID", defaultPermissions("ROAD_GRID"));
        ensureUser("grid", "123456", "网格员", "GRID_MEMBER", defaultPermissions("GRID_MEMBER"));
        ensureUser("squad", "123456", "交警中队", "TRAFFIC_SQUAD", defaultPermissions("TRAFFIC_SQUAD"));
        ensureUser("fire", "123456", "消防联动员", "FIRE", defaultPermissions("FIRE"));
    }

    private void ensureUser(String username, String password, String realName, String role, String permissions) {
        java.util.Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getMenuPermissions() == null || user.getMenuPermissions().trim().isEmpty()) {
                user.setMenuPermissions(permissions);
                userRepository.save(user);
            } else if ("ADMIN".equalsIgnoreCase(role) && !user.getMenuPermissions().contains("view_operation_logs")) {
                user.setMenuPermissions(user.getMenuPermissions() + ",view_operation_logs");
                userRepository.save(user);
            }
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setRole(role);
        user.setMenuPermissions(permissions);
        user.setCreateTime(new Date());
        userRepository.save(user);
    }

    private String defaultPermissions(String role) {
        String r = role == null ? "USER" : role.toUpperCase();
        if ("ADMIN".equals(r)) {
            return "view_home,view_violations,view_pending,view_add_violation,view_statistics,view_intelligent,view_warnings,view_cameras,view_user_manage,view_operation_logs";
        }
        if ("OFFICER".equals(r)) {
            return "view_home,view_violations,view_pending,view_add_violation,view_warnings,view_cameras";
        }
        if ("ANALYST".equals(r)) {
            return "view_home,view_statistics,view_intelligent,view_warnings";
        }
        if ("ROAD_GRID".equals(r) || "GRID_MEMBER".equals(r) || "TRAFFIC_SQUAD".equals(r) || "FIRE".equals(r)) {
            return "view_home,view_warnings";
        }
        return "view_home,view_violations";
    }
}
