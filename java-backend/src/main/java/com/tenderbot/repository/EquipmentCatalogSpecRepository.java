package com.tenderbot.repository;

import com.tenderbot.entity.EquipmentCatalogSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentCatalogSpecRepository extends JpaRepository<EquipmentCatalogSpec, Long> {
    List<EquipmentCatalogSpec> findByCatalogItemId(Long catalogItemId);
    Optional<EquipmentCatalogSpec> findByCatalogItemIdAndCharacteristicId(Long catalogItemId, Long characteristicId);
    void deleteByCatalogItemId(Long catalogItemId);
}
