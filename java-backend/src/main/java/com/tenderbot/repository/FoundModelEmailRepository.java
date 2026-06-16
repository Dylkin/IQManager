package com.tenderbot.repository;

import com.tenderbot.entity.FoundModelEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoundModelEmailRepository extends JpaRepository<FoundModelEmail, Long> {
    List<FoundModelEmail> findByFoundModelIdOrderByCreatedAtDesc(Long foundModelId);
    boolean existsByMessageId(String messageId);
    void deleteByFoundModelIdIn(List<Long> foundModelIds);
}
