package com.lantanagroup.link.cli;

import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli.manual-bed-inventory")
public class ManualBedInventoryConfig {
    @NotNull
    private String profileUrl;

    @NotNull
    private String measureUrl;

    @NotNull
    private String subjectIdentifier;

    @NotNull
    private String bedInventoryReportId;

    @NotNull
    private ApiDataStoreConfig dataStore;
}
