package com.zhihui.bishe.controller;

import com.zhihui.bishe.model.Violation;
import com.zhihui.bishe.service.ViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private ViolationService violationService;

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        List<Violation> violations = violationService.getAllViolations();

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalViolations", violations.size());
        overview.put("totalFines", violations.stream()
                .mapToDouble(v -> v.getFineAmount() != null ? v.getFineAmount() : 0)
                .sum());
        overview.put("totalPoints", violations.stream()
                .mapToInt(v -> v.getPointsDeducted() != null ? v.getPointsDeducted() : 0)
                .sum());
        overview.put("averageFine",
                violations.isEmpty() ? 0 :
                        violations.stream()
                                .mapToDouble(v -> v.getFineAmount() != null ? v.getFineAmount() : 0)
                                .average()
                                .orElse(0));

        return overview;
    }

    @GetMapping("/by-type")
    public Map<String, Object> getStatisticsByType() {
        List<Violation> violations = violationService.getAllViolations();

        Map<String, Long> typeCount = violations.stream()
                .filter(v -> v.getViolationType() != null)
                .collect(Collectors.groupingBy(
                        Violation::getViolationType,
                        Collectors.counting()
                ));

        Map<String, Double> typeFines = violations.stream()
                .filter(v -> v.getViolationType() != null && v.getFineAmount() != null)
                .collect(Collectors.groupingBy(
                        Violation::getViolationType,
                        Collectors.summingDouble(Violation::getFineAmount)
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("typeCount", typeCount);
        result.put("typeFines", typeFines);

        return result;
    }

    @GetMapping("/by-month")
    public Map<String, Object> getStatisticsByMonth() {
        List<Violation> violations = violationService.getAllViolations();

        Map<String, Long> monthlyCount = violations.stream()
                .filter(v -> v.getViolationTime() != null)
                .collect(Collectors.groupingBy(
                        v -> {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(v.getViolationTime());
                            return String.format("%d-%02d",
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH) + 1);
                        },
                        Collectors.counting()
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyCount", monthlyCount);
        return result;
    }

    @GetMapping("/top-plates")
    public List<Map<String, Object>> getTopPlates(
            @RequestParam(defaultValue = "10") int limit) {

        List<Violation> violations = violationService.getAllViolations();

        Map<String, Long> plateCount = violations.stream()
                .filter(v -> v.getPlateNumber() != null)
                .collect(Collectors.groupingBy(
                        Violation::getPlateNumber,
                        Collectors.counting()
                ));

        return plateCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("plateNumber", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }
}


