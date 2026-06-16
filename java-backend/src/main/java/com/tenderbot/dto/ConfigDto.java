package com.tenderbot.dto;

import com.tenderbot.entity.ConfigGroup;

public class ConfigDto {
    private Long id;
    private String key;
    private String value;
    private String description;
    private ConfigGroup group;
    private Boolean isSecret;
    private Boolean isEditable;
    private String createdAt;
    private String updatedAt;

    public ConfigDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ConfigGroup getGroup() { return group; }
    public void setGroup(ConfigGroup group) { this.group = group; }

    public Boolean getIsSecret() { return isSecret; }
    public void setIsSecret(Boolean isSecret) { this.isSecret = isSecret; }

    public Boolean getIsEditable() { return isEditable; }
    public void setIsEditable(Boolean isEditable) { this.isEditable = isEditable; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
