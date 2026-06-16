package com.tenderbot.repository;

import com.tenderbot.entity.EquipmentTypeCharacteristic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentTypeCharacteristicRepository extends JpaRepository<EquipmentTypeCharacteristic, Long> {
    List<EquipmentTypeCharacteristic> findByEquipmentTypeId(Long equipmentTypeId);
    Optional<EquipmentTypeCharacteristic> findByEquipmentTypeIdAndKeyIgnoreCase(Long equipmentTypeId, String key);
}
