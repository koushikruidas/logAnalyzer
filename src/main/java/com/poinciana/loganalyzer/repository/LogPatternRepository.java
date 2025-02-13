package com.poinciana.loganalyzer.repository;

import com.poinciana.loganalyzer.entity.LogPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LogPatternRepository extends JpaRepository<LogPattern, Long> {
    Optional<LogPattern> findByIsDefaultTrue();
}