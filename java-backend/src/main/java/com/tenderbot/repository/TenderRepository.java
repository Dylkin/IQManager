package com.tenderbot.repository;

import com.tenderbot.entity.Tender;
import com.tenderbot.entity.TenderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {
    Optional<Tender> findByTenderNumber(String tenderNumber);
    List<Tender> findByStatus(TenderStatus status);
    List<Tender> findByStatusOrderByCreatedAtDesc(TenderStatus status);

    @Query("SELECT t FROM Tender t ORDER BY t.createdAt DESC")
    List<Tender> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(t) FROM Tender t WHERE t.status = ?1")
    long countByStatus(TenderStatus status);

    @Query("SELECT t FROM Tender t LEFT JOIN FETCH t.items WHERE t.id = :id")
    Optional<Tender> findByIdWithItems(@Param("id") Long id);
}
