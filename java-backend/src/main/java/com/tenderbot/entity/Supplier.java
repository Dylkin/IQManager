package com.tenderbot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "search_url_template")
    private String searchUrlTemplate;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "search_selector_product")
    private String searchSelectorProduct;

    @Column(name = "search_selector_price")
    private String searchSelectorPrice;

    @Column(name = "search_selector_link")
    private String searchSelectorLink;

    public Supplier() {}


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getSearchUrlTemplate() {
        return searchUrlTemplate;
    }

    public void setSearchUrlTemplate(String searchUrlTemplate) {
        this.searchUrlTemplate = searchUrlTemplate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSearchSelectorProduct() {
        return searchSelectorProduct;
    }

    public void setSearchSelectorProduct(String searchSelectorProduct) {
        this.searchSelectorProduct = searchSelectorProduct;
    }

    public String getSearchSelectorPrice() {
        return searchSelectorPrice;
    }

    public void setSearchSelectorPrice(String searchSelectorPrice) {
        this.searchSelectorPrice = searchSelectorPrice;
    }

    public String getSearchSelectorLink() {
        return searchSelectorLink;
    }

    public void setSearchSelectorLink(String searchSelectorLink) {
        this.searchSelectorLink = searchSelectorLink;
    }

}
