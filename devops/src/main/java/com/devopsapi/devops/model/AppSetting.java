package com.devopsapi.devops.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    private String settingKey;
    private String settingValue;

    public AppSetting() {}

    public AppSetting(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }

    public String getKey() {
        return settingKey;
    }

    public void setKey(String key) {
        this.settingKey = key;
    }

    public String getValue() {
        return settingValue;
    }

    public void setValue(String value) {
        this.settingValue = value;
    }
}
