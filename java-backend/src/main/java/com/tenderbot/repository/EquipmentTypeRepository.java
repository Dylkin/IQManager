package com.tenderbot.repository;

import com.tenderbot.entity.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {
    Optional<EquipmentType> findByNameIgnoreCase(String name);
    Optional<EquipmentType> findByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
}
