package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Validated
public class ApiReportDefsBundleConfig {
    /**
     * <strong>api.report-defs.bundles.bundle-id</strong><br>ID of stored Bundle.
     */
    @NotBlank
    private String bundleId;

    /**
     * <strong>api.report-defs.bundles.report-aggregator</strong><br>Aggregator used to aggregate for that measure.
     */
    private String reportAggregator;

}
