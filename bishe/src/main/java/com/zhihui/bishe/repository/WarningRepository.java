package com.zhihui.bishe.repository;

import com.zhihui.bishe.model.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    org.springframework.data.domain.Page<Warning> findAllByOrderByWarningTimeDesc(
            org.springframework.data.domain.Pageable pageable);
}
