package com.tenderbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tender_items")
public class TenderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "original_description", length = 4000)
    private String originalDescription;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "estimated_price")
    private Double estimatedPrice;

    @Column(name = "currency")
    private String currency;

    @Column(name = "okpd2_code")
    private String okpd2Code;

    @Column(name = "found_model_name")
    private String foundModelName;

    @Column(name = "found_model_url")
    private String foundModelUrl;

    @Column(name = "found_model_price")
    private Double foundModelPrice;

    @Column(name = "delivery_cost_byn")
    private Double deliveryCostByn;

    @Column(name = "markup_percent")
    private Double markupPercent;

    @Column(name = "supplier_site")
    private String supplierSite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ItemStatus status;

    @Column(name = "document_description", length = 4000)
    private String documentDescription;

    @Column(name = "document_file_url", length = 2000)
    private String documentFileUrl;

    @Column(name = "document_file_name", length = 500)
    private String documentFileName;

    @Column(name = "extracted_params", length = 4000)
    private String extractedParams;

    @Column(name = "match_score")
    private Double matchScore;

    @OneToMany(mappedBy = "tenderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rankPosition ASC")
    private java.util.List<FoundModel> foundModels = new java.util.ArrayList<>();

    public TenderItem() {}


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tender getTender() {
        return tender;
    }

    public void setTender(Tender tender) {
        this.tender = tender;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    public void setOriginalDescription(String originalDescription) {
        this.originalDescription = originalDescription;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(Double estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOkpd2Code() {
        return okpd2Code;
    }

    public void setOkpd2Code(String okpd2Code) {
        this.okpd2Code = okpd2Code;
    }

    public String getFoundModelName() {
        return foundModelName;
    }

    public void setFoundModelName(String foundModelName) {
        this.foundModelName = foundModelName;
    }

    public String getFoundModelUrl() {
        return foundModelUrl;
    }

    public void setFoundModelUrl(String foundModelUrl) {
        this.foundModelUrl = foundModelUrl;
    }

    public Double getFoundModelPrice() {
        return foundModelPrice;
    }

    public void setFoundModelPrice(Double foundModelPrice) {
        this.foundModelPrice = foundModelPrice;
    }

    public Double getDeliveryCostByn() {
        return deliveryCostByn;
    }

    public void setDeliveryCostByn(Double deliveryCostByn) {
        this.deliveryCostByn = deliveryCostByn;
    }

    public Double getMarkupPercent() {
        return markupPercent;
    }

    public void setMarkupPercent(Double markupPercent) {
        this.markupPercent = markupPercent;
    }

    public String getSupplierSite() {
        return supplierSite;
    }

    public void setSupplierSite(String supplierSite) {
        this.supplierSite = supplierSite;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public String getDocumentDescription() {
        return documentDescription;
    }

    public void setDocumentDescription(String documentDescription) {
        this.documentDescription = documentDescription;
    }

    public String getDocumentFileUrl() {
        return documentFileUrl;
    }

    public void setDocumentFileUrl(String documentFileUrl) {
        this.documentFileUrl = documentFileUrl;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public String getExtractedParams() {
        return extractedParams;
    }

    public void setExtractedParams(String extractedParams) {
        this.extractedParams = extractedParams;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public java.util.List<FoundModel> getFoundModels() {
        return foundModels;
    }

    public void setFoundModels(java.util.List<FoundModel> foundModels) {
        this.foundModels = foundModels;
    }

}
