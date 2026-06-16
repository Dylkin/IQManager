package com.tenderbot.repository;

import com.tenderbot.entity.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {
    List<ProcessingLog> findByTenderIdOrderByCreatedAtDesc(Long tenderId);
    List<ProcessingLog> findTop100ByOrderByCreatedAtDesc();
}
