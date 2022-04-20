package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiConfig {
    /**
     * <strong>api.measure-location</strong><br>Location information to be included in all MeasureReport resources exported/sent from the system
     */
    @Getter
    ApiMeasureLocationConfig measureLocation;

    /**
     * <strong>api.skip-init</strong><br>If true, init processes (loading measure bundles and resources into the internal FHIR server) should be skipped
     */
    private Boolean skipInit = false;

    /**
     * <strong>api.fhir-server-store</strong><br>URL where the FHIR server is that is used for storage
     */
    @NotNull
    private String fhirServerStore;

    /**
     * <strong>evaluation-service</strong><br>The measure evaluation service (CQF-Ruler) installation that is to be used to evaluate patient data against measure logic.
     */
    @Getter @Setter @NotNull
    private String evaluationService;

    /**
     * <strong>api.terminology-service</strong><br>The FHIR terminology service to use for storing ValueSet and CodeSystem resources, passed to the evaluation-service for use during measure evaluation.
     */
    @Getter @Setter @NotNull
    private String terminologyService;

    /**
     * <strong>api.auth-jwks-url</strong><br>URL used to retrieve certs from identity provider to verify that JWT's are valid
     */
    private String authJwksUrl;

    /**
     * <strong>api.downloader</strong><br>The class used to download reports
     */
    @NotNull
    private String downloader;

    /**
     * <strong>api.sender</strong><br>The class used to send reports
     */
    @NotNull
    private String sender;

    /**
     * <strong>api.send-whole-bundle</strong><br>Boolean used to determine if the full Bundle is sent or just the MeasureReport. True to send full bundle and false to send just the MeasureReport
     */
    private Boolean sendWholeBundle;

    /**
     * <strong>api.patient-id-resolver</strong><br>The class used to determine the list of patient ids that should be queried for
     */
    private String patientIdResolver;

    /**
     * <strong>api.document-reference-system</strong><br>The "system" value of identifiers for DocumentReference resources created to index reports generated
     */
    private String documentReferenceSystem;

    /**
     * <strong>api.cors</strong><br>CORS configuration used for browser interaction with the API
     */
    @Getter
    private ApiCorsConfig cors;

    /**
     * <strong>api.report-defs</strong><br>Configuration for report definitions supported by the system
     */
    @Getter
    private ApiReportDefsConfig reportDefs;

    /**
     * <strong>api.query</strong><br>Configuration for how queries should be executed. If local, will run queries within the API. If remote,
     * will request that a remote query agent perform the queries and respond to the API with the results.
     */
    @Getter @NotNull
    private ApiQueryConfig query;

    /**
     * <strong>api.user</strong><br>Configuration related to the user that is responsible for running the installation of Link, such as timezone settings.
     */
    @Getter
    private UserConfig user;

    /**
     * <strong>api.concept-maps</strong><br>API configuration to indicate one or more ConceptMaps to apply to patient data
     */
    @Getter
    private List<String> conceptMaps;

}
