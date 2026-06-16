package com.tenderbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment_type_characteristics")
public class EquipmentTypeCharacteristic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @Column(name = "\"key\"", nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 50)
    private String unit;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public EquipmentTypeCharacteristic() {}

    public EquipmentTypeCharacteristic(EquipmentType equipmentType, String key, String label, String unit, Integer sortOrder) {
        this.equipmentType = equipmentType;
        this.key = key;
        this.label = label;
        this.unit = unit;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EquipmentType getEquipmentType() { return equipmentType; }
    public void setEquipmentType(EquipmentType equipmentType) { this.equipmentType = equipmentType; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
