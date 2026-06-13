package com.zhihui.bishe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DatabaseInfoService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInfoService.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            info.put("host", scalar("SELECT @@hostname"));
            info.put("port", scalar("SELECT @@port"));
            info.put("database", scalar("SELECT DATABASE()"));
            info.put("datadir", scalar("SELECT @@datadir"));
            info.put("violation_count", scalar("SELECT COUNT(*) FROM traffic_violation"));
            info.put("max_violation_id", scalar("SELECT COALESCE(MAX(violation_id), 0) FROM traffic_violation"));
            info.put("status", "ok");
        } catch (Exception e) {
            info.put("status", "error");
            info.put("message", e.getMessage());
        }
        return info;
    }

    private Object scalar(String sql) {
        return jdbcTemplate.queryForObject(sql, Object.class);
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> info = getDatabaseInfo();
        log.info("=== Current DB Connection Info ===");
        log.info("status={}", info.get("status"));
        if ("ok".equals(info.get("status"))) {
            log.info("host={} port={} database={}", info.get("host"), info.get("port"), info.get("database"));
            log.info("datadir={}", info.get("datadir"));
            log.info("traffic_violation rows={} max_violation_id={}", info.get("violation_count"), info.get("max_violation_id"));
        } else {
            log.warn("database info error: {}", info.get("message"));
        }
    }
}
