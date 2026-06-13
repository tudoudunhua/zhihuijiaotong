package com.zhihui.bishe.repository;

import com.zhihui.bishe.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    /**
     * 统计某时间段内各地点的违章次数（高发点分析）
     */
    @Query(
        "SELECT v.violationLocation, COUNT(v) " +
        "FROM Violation v " +
        "WHERE v.violationTime BETWEEN :startTime AND :endTime " +
        "GROUP BY v.violationLocation " +
        "HAVING COUNT(v) >= :threshold"
    )
    List<Object[]> findPeakHourViolations(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("threshold") Long threshold
    );

    @Query(
        "SELECT v.violationLocation, COUNT(v) " +
        "FROM Violation v " +
        "WHERE v.violationTime >= :since " +
        "GROUP BY v.violationLocation " +
        "ORDER BY COUNT(v) DESC"
    )
    List<Object[]> findTopLocationsSince(@Param("since") Date since, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT v.violationType FROM Violation v")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT v.violationLocation FROM Violation v")
    List<String> findDistinctLocations();

    /**
     * 按筛选条件统计各地点违章数量（用于智能预警热点图，真实数据优先）
     */
    @Query(
        "SELECT v.violationLocation, COUNT(v) " +
        "FROM Violation v " +
        "WHERE (:type = '0' OR v.violationType = :type) " +
        "  AND (:location = '0' OR v.violationLocation LIKE CONCAT('%', :location, '%')) " +
        "  AND (:hour < 0 OR FUNCTION('HOUR', v.violationTime) = :hour) " +
        "GROUP BY v.violationLocation " +
        "ORDER BY COUNT(v) DESC"
    )
    List<Object[]> findTopLocationsByFilters(
            @Param("hour") int hour,
            @Param("type") String type,
            @Param("location") String location,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * 按筛选条件统计 24 小时分布（用于趋势图，真实数据优先）
     */
    @Query(
        "SELECT FUNCTION('HOUR', v.violationTime), COUNT(v) " +
        "FROM Violation v " +
        "WHERE (:type = '0' OR v.violationType = :type) " +
        "  AND (:location = '0' OR v.violationLocation LIKE CONCAT('%', :location, '%')) " +
        "GROUP BY FUNCTION('HOUR', v.violationTime) " +
        "ORDER BY FUNCTION('HOUR', v.violationTime)"
    )
    List<Object[]> findHourlyCountsByFilters(
            @Param("type") String type,
            @Param("location") String location
    );

    /**
     * 近一段时间复犯车辆统计：按车牌聚合
     */
    @Query(
        "SELECT v.plateNumber, COUNT(v), COUNT(DISTINCT v.violationType), MAX(v.violationTime) " +
        "FROM Violation v " +
        "WHERE v.violationTime >= :since " +
        "  AND (:location = '0' OR v.violationLocation LIKE CONCAT('%', :location, '%')) " +
        "GROUP BY v.plateNumber " +
        "HAVING COUNT(v) >= :minCount " +
        "ORDER BY COUNT(v) DESC, COUNT(DISTINCT v.violationType) DESC, MAX(v.violationTime) DESC"
    )
    List<Object[]> findRepeatOffendersSince(
            @Param("since") Date since,
            @Param("location") String location,
            @Param("minCount") Long minCount,
            org.springframework.data.domain.Pageable pageable
    );

    /** 待处理违章（disposeStatus 为空或为 0） */
    @Query("SELECT v FROM Violation v WHERE (v.disposeStatus IS NULL OR v.disposeStatus = 0) ORDER BY v.violationTime DESC")
    List<Violation> findPendingOrderByViolationTimeDesc();
}
