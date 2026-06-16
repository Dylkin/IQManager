package com.tenderbot.repository;

import com.tenderbot.entity.TenderItem;
import com.tenderbot.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenderItemRepository extends JpaRepository<TenderItem, Long> {
    List<TenderItem> findByTenderId(Long tenderId);
    List<TenderItem> findByStatus(ItemStatus status);
    List<TenderItem> findBySupplierSite(String supplierSite);

    @Query("SELECT ti FROM TenderItem ti LEFT JOIN FETCH ti.foundModels WHERE ti.id = :id")
    Optional<TenderItem> findByIdWithFoundModels(Long id);
}
