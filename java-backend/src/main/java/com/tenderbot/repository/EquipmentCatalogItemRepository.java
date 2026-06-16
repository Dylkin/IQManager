package com.tenderbot.repository;

import com.tenderbot.entity.EquipmentCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentCatalogItemRepository extends JpaRepository<EquipmentCatalogItem, Long> {
    List<EquipmentCatalogItem> findByEquipmentTypeId(Long equipmentTypeId);
    List<EquipmentCatalogItem> findByManufacturerId(Long manufacturerId);
    List<EquipmentCatalogItem> findByEquipmentTypeIdAndManufacturerId(Long equipmentTypeId, Long manufacturerId);
    Optional<EquipmentCatalogItem> findByEquipmentTypeIdAndManufacturerIdAndModelNameIgnoreCase(
            Long equipmentTypeId, Long manufacturerId, String modelName);

    Optional<EquipmentCatalogItem> findByEquipmentTypeIdAndModelNameIgnoreCase(
            Long equipmentTypeId, String modelName);

    List<EquipmentCatalogItem> findByEquipmentTypeIdAndModelNameContainingIgnoreCase(
            Long equipmentTypeId, String modelName);
}
