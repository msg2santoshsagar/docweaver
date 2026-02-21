package com.docweaver.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_config")
public class AppConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private String outputFolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutputType defaultStandaloneOutputType;

    @Column(nullable = false)
    private Boolean defaultDeleteOriginals;

    @Column(nullable = false)
    private Boolean dryRun;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_config_categories", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "category", nullable = false)
    private List<String> categories = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public OutputType getDefaultStandaloneOutputType() {
        return defaultStandaloneOutputType;
    }

    public void setDefaultStandaloneOutputType(OutputType defaultStandaloneOutputType) {
        this.defaultStandaloneOutputType = defaultStandaloneOutputType;
    }

    public Boolean getDefaultDeleteOriginals() {
        return defaultDeleteOriginals;
    }

    public void setDefaultDeleteOriginals(Boolean defaultDeleteOriginals) {
        this.defaultDeleteOriginals = defaultDeleteOriginals;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
