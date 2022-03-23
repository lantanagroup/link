package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.IDirectConfig;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "api")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiConfig {
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
     * <strong>evaluation-service: https://cqf-ruler.nhsnlink.org/fhir</strong>
     */
    @Getter
    @Setter
    private String evaluationService;

    /**
     * <strong>api.auth-jwks-url</strong><br>URL used to retrieve certs from identity provider to verify that JWT's are valid
     */
    private String authJwksUrl;

    /**
     * <strong>api.downloader</strong><br>The class used to download reports
     */
    private String downloader;

    /**
     * <strong>api.sender</strong><br>The class used to send reports
     */
    private String sender;

    /**
     * <string>api.send-whole-bundle</string><br>Boolean used to determine if the full Bundle is sent or just the MeasureReport. True to send full bundle and false to send just the MeasureReport
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
     * <strong>api.terminology</strong><br>Configuration specific to terminology, such as value sets
     */
    @Getter
    private ApiTerminologyConfig terminology;

    @Getter
    private IDirectConfig direct;

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
     * <strong>api.measure-location</strong><br>Location information to be included in all MeasureReport resources exported/sent from the system
     */
    @Getter ApiMeasureLocationConfig measureLocation;

    @Getter
    private UserConfig user;

}
