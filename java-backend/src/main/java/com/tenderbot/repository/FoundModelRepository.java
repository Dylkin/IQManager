package com.tenderbot.repository;

import com.tenderbot.entity.FoundModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoundModelRepository extends JpaRepository<FoundModel, Long> {
    List<FoundModel> findByTenderItemIdOrderByRankPositionAsc(Long tenderItemId);
    void deleteByTenderItemId(Long tenderItemId);
}
