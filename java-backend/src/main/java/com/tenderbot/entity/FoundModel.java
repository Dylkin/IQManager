package com.tenderbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "found_models")
public class FoundModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_item_id")
    private TenderItem tenderItem;

    @Column(name = "product_name", length = 1000)
    private String productName;

    @Column(name = "product_url", length = 2000)
    private String productUrl;

    @Column(name = "price")
    private Double price;

    @Column(name = "supplier_site", length = 500)
    private String supplierSite;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "semantic_score")
    private Double semanticScore;

    @Column(name = "parametric_score")
    private Double parametricScore;

    @Column(name = "rank_position")
    private Integer rankPosition;

    public FoundModel() {}

    public FoundModel(TenderItem tenderItem, String productName, String productUrl, Double price,
                      String supplierSite, Double matchScore, Double semanticScore,
                      Double parametricScore, Integer rankPosition) {
        this.tenderItem = tenderItem;
        this.productName = productName;
        this.productUrl = productUrl;
        this.price = price;
        this.supplierSite = supplierSite;
        this.matchScore = matchScore;
        this.semanticScore = semanticScore;
        this.parametricScore = parametricScore;
        this.rankPosition = rankPosition;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TenderItem getTenderItem() { return tenderItem; }
    public void setTenderItem(TenderItem tenderItem) { this.tenderItem = tenderItem; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getSupplierSite() { return supplierSite; }
    public void setSupplierSite(String supplierSite) { this.supplierSite = supplierSite; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public Double getSemanticScore() { return semanticScore; }
    public void setSemanticScore(Double semanticScore) { this.semanticScore = semanticScore; }

    public Double getParametricScore() { return parametricScore; }
    public void setParametricScore(Double parametricScore) { this.parametricScore = parametricScore; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }
}
