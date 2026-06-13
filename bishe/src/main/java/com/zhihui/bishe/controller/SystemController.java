package com.zhihui.bishe.controller;

import com.zhihui.bishe.service.DatabaseInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final DatabaseInfoService databaseInfoService;

    public SystemController(DatabaseInfoService databaseInfoService) {
        this.databaseInfoService = databaseInfoService;
    }

    @GetMapping("/db-info")
    public Map<String, Object> dbInfo() {
        return databaseInfoService.getDatabaseInfo();
    }
}
