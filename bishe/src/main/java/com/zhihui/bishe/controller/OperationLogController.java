package com.zhihui.bishe.controller;

import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.util.RoleUtil;
import com.zhihui.bishe.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/operation-logs")
@CrossOrigin
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String username,
                                  @RequestParam(required = false) String module,
                                  @RequestParam(required = false) String action,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("仅管理员可查看操作日志");
        }
        Map<String, Object> data = operationLogService.query(
                username, module, action, status, startTime, endTime, page, size
        );
        return ResponseEntity.ok(data);
    }

    private boolean isAdmin(HttpServletRequest request) {
        return RoleUtil.isAdmin(JwtUtil.getRoleFromToken(request.getHeader("Authorization")));
    }
}
