package com.poinciana.loganalyzer.repository;

import com.poinciana.loganalyzer.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByLevel(String level);

    List<LogEntry> findByServiceName(String serviceName);

    @Query("SELECT l FROM LogEntry l WHERE (:level IS NULL OR l.level = :level) " +
            "AND (:serviceName IS NULL OR l.serviceName = :serviceName) " +
            "AND (:startDate IS NULL OR l.timestamp >= :startDate) " +
            "AND (:endDate IS NULL OR l.timestamp <= :endDate)")
    List<LogEntry> findFilteredLogs(
            @Param("level") String level,
            @Param("serviceName") String serviceName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

