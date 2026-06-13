package com.zhihui.bishe.controller;

import com.zhihui.bishe.dto.ViolationDTO;
import com.zhihui.bishe.model.Warning;
import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.service.WarningSimulationService;
import com.zhihui.bishe.service.ViolationSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/simulate")
@CrossOrigin
public class SimulationController {

    @Autowired
    private ViolationSimulationService simulationService;

    @Autowired
    private WarningSimulationService warningSimulationService;

    /**
     * 生成“摄像头自动抓拍”的待处理违章
     * 示例：POST /api/simulate/captured-violations?count=10
     */
    @PostMapping("/captured-violations")
    public List<ViolationDTO> generateCapturedViolations(
            @RequestParam(name = "count", defaultValue = "10") int count,
            HttpServletRequest request
    ) {
        // 模拟数据仅用于演示：默认不脱敏，方便管理员查看；若你希望遵守角色脱敏，可改为按 JwtUtil 判断
        List<Violation> created = simulationService.generateCapturedViolations(count);
        return created.stream().map(v -> ViolationDTO.from(v, false)).collect(Collectors.toList());
    }

    /**
     * 生成演示预警：蓝/黄/橙/红各 perLevel 条
     * 示例：POST /api/simulate/demo-warnings?perLevel=5
     */
    @PostMapping("/demo-warnings")
    public List<Warning> generateDemoWarnings(
            @RequestParam(name = "perLevel", defaultValue = "5") int perLevel
    ) {
        return warningSimulationService.generateDemoWarnings(perLevel);
    }
}

