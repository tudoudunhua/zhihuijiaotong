package com.zhihui.bishe.repository;

import com.zhihui.bishe.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Optional<NotificationLog> findTopByViolationIdOrderByCreatedTimeDesc(Long violationId);
    List<NotificationLog> findByViolationIdOrderByCreatedTimeDesc(Long violationId);
}

