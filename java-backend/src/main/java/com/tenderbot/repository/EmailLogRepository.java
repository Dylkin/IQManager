package com.tenderbot.repository;

import com.tenderbot.entity.EmailLog;
import com.tenderbot.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByTenderItemId(Long tenderItemId);
    List<EmailLog> findByStatus(EmailStatus status);
    List<EmailLog> findTop50ByOrderByCreatedAtDesc();
}
