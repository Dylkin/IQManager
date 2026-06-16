package com.tenderbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_catalog_specs")
public class EquipmentCatalogSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private EquipmentCatalogItem catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "characteristic_id", nullable = false)
    private EquipmentTypeCharacteristic characteristic;

    @Column(name = "\"value\"", nullable = false, length = 500)
    private String value;

    public EquipmentCatalogSpec() {}

    public EquipmentCatalogSpec(EquipmentCatalogItem catalogItem, EquipmentTypeCharacteristic characteristic, String value) {
        this.catalogItem = catalogItem;
        this.characteristic = characteristic;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EquipmentCatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(EquipmentCatalogItem catalogItem) { this.catalogItem = catalogItem; }

    public EquipmentTypeCharacteristic getCharacteristic() { return characteristic; }
    public void setCharacteristic(EquipmentTypeCharacteristic characteristic) { this.characteristic = characteristic; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
