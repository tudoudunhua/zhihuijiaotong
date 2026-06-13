package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.Warning;
import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.service.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    @Autowired
    private WarningService warningService;

    /** 预警列表（含四级 level、pushTarget） */
    @GetMapping
    public List<Warning> list(HttpServletRequest request) {
        String role = JwtUtil.getRoleFromToken(request.getHeader("Authorization"));
        return warningService.getLatestWarningsByRole(role);
    }

    /** 早晚高峰违章高发预警接口（触发生成） */
    @GetMapping("/peak")
    public List<Warning> peakHourWarnings() {
        return warningService.generatePeakHourWarnings();
    }
}
