package com.zhihui.bishe.service;

import com.zhihui.bishe.model.OperationLog;
import com.zhihui.bishe.model.User;
import com.zhihui.bishe.repository.OperationLogRepository;
import com.zhihui.bishe.repository.UserRepository;
import com.zhihui.bishe.security.JwtUtil;
import com.zhihui.bishe.util.OperationLogDescriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final UserRepository userRepository;

    public OperationLogService(OperationLogRepository operationLogRepository,
                               UserRepository userRepository) {
        this.operationLogRepository = operationLogRepository;
        this.userRepository = userRepository;
    }

    public void recordLogin(String username, boolean success, String detail, String clientIp, User user) {
        OperationLog log = new OperationLog();
        log.setModule("AUTH");
        log.setAction(success ? "用户登录" : "用户登录失败");
        log.setHttpMethod("POST");
        log.setRequestUri("/api/auth/login");
        log.setUsername(username);
        log.setStatus(success ? "SUCCESS" : "FAIL");
        log.setHttpStatus(success ? 200 : 401);
        log.setClientIp(clientIp);
        log.setDetail(truncate(detail, 500));
        log.setCreatedTime(new Date());
        if (user != null) {
            log.setRealName(user.getRealName());
            log.setRole(user.getRole());
        }
        operationLogRepository.save(log);
    }

    public void record(HttpServletRequest request, int httpStatus, Exception ex) {
        if (request == null) {
            return;
        }
        String method = request.getMethod();
        if (method == null) {
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return;
        }

        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/operation-logs") || uri.startsWith("/api/auth/login"))) {
            return;
        }

        OperationLogDescriber.DescribedOperation described =
                OperationLogDescriber.describe(method, uri);

        OperationLog log = new OperationLog();
        log.setHttpMethod(method.toUpperCase());
        log.setRequestUri(uri);
        log.setModule(described.getModule());
        log.setAction(described.getAction());
        log.setHttpStatus(httpStatus);
        log.setClientIp(resolveClientIp(request));
        log.setCreatedTime(new Date());

        boolean success = httpStatus > 0 && httpStatus < 400 && ex == null;
        log.setStatus(success ? "SUCCESS" : "FAIL");
        if (!success && ex != null && ex.getMessage() != null) {
            log.setDetail(truncate(ex.getMessage(), 500));
        }

        String auth = request.getHeader("Authorization");
        String username = JwtUtil.getUsernameFromAuthorization(auth);
        String role = JwtUtil.getRoleFromToken(auth);
        log.setUsername(username);
        log.setRole(role);

        if (StringUtils.hasText(username)) {
            Optional<User> userOpt = userRepository.findByUsername(username.trim());
            userOpt.ifPresent(u -> log.setRealName(u.getRealName()));
        }

        operationLogRepository.save(log);
    }

    public Map<String, Object> query(String username,
                                     String module,
                                     String action,
                                     String status,
                                     String startTime,
                                     String endTime,
                                     int page,
                                     int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(root.get("username"), "%" + username.trim() + "%"));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(cb.upper(root.get("module")), module.trim().toUpperCase()));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.like(root.get("action"), "%" + action.trim() + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            Date start = parseDateTime(startTime);
            Date end = parseDateTime(endTime);
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OperationLog> pageData = operationLogRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdTime"))
        );

        List<Map<String, Object>> items = new ArrayList<>();
        for (OperationLog log : pageData.getContent()) {
            items.add(toMap(log));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("total", pageData.getTotalElements());
        res.put("page", safePage);
        res.put("size", safeSize);
        return res;
    }

    private Map<String, Object> toMap(OperationLog log) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logId", log.getLogId());
        m.put("username", log.getUsername());
        m.put("realName", log.getRealName());
        m.put("role", log.getRole());
        m.put("module", log.getModule());
        m.put("action", log.getAction());
        m.put("httpMethod", log.getHttpMethod());
        m.put("requestUri", log.getRequestUri());
        m.put("clientIp", log.getClientIp());
        m.put("status", log.getStatus());
        m.put("httpStatus", log.getHttpStatus());
        m.put("detail", log.getDetail());
        m.put("createdTime", log.getCreatedTime());
        return m;
    }

    private Date parseDateTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(text.trim());
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
