package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiConfig {

  /**
   * <strong>api.validate-fhir-server</strong><br>Boolean for whether to check for metadata before request or not
   */
  @Getter
  private Boolean validateFhirServer;

  /**
   *<strong>api.tenant-id<strong/><br>Tenant ID for the facility
   */
  @Getter
  private String tenantID;

  /**
   * <strong>api.public-address</strong><br>The public endpoint address for the API (i.e. https://dev.nhsnlink.org/api)
   */
  @Getter
  private String publicAddress;

  /**
   * <strong>api.require-https</strong><br>Indicates if HTTPS is required for submission urls.
   */
  @Getter
  private boolean requireHttps;

  /**
   * <strong>api.measure-location</strong><br>Location information to be included in all MeasureReport resources exported/sent from the system
   */
  @Getter
  private ApiMeasureLocationConfig measureLocation;

  /**
   * <strong>api.skip-init</strong><br>If true, init processes (loading measure bundles and resources into the internal FHIR server) should be skipped
   */
  private Boolean skipInit = false;

  /**
   * <strong>api.max-retry</strong><br>The number of times the API should try to check that prerequisite services are available. If the retry value is null the check will run indefinitely.
   */
  @PositiveOrZero
  public Integer maxRetry;

  /**
   * <strong>api.retry-wait</strong><br>The number of milliseconds the API should wait in between attempts at checking that prerequisite services are available.
   */
  @PositiveOrZero
  public int retryWait = 5000;


  /**
   * <strong>api.data-store</strong><br>Required. Defines the location and authentication for the data storage service.
   */
  @Getter @NotNull
  private ApiDataStoreConfig dataStore;

  /**
   * <strong>evaluation-service</strong><br>The measure evaluation service (CQF-Ruler) installation that is to be used to evaluate patient data against measure logic.
   */
  @Getter @Setter @NotNull
  private String evaluationService;

  /**
   * <strong>api.terminology-service</strong><br>The FHIR terminology service to use for storing ValueSet and CodeSystem resources, passed to the evaluation-service for use during measure evaluation.
   */
  @Getter
  @Setter
  @NotNull
  private String terminologyService;

  /**
   * <strong>api.auth-jwks-url</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
   */
  @Getter
  @Setter
  private String authJwksUrl;

  /**
   * <strong>api.issuer</strong><br>This issuer is used during token validation to ensure that the JWT has been issued by a trusted system
   */
  @Getter
  @Setter
  private String issuer;

  /**
   * <strong>api.algorithm</strong><br>The algorithm used by the identity provider to sign the jwt token
   */
  @Getter
  @Setter
  private String algorithm;

  /**
   * <strong>api.tokenVerificationClass</strong><br>The class configured to verify a jwt token in the api
   */
  @Getter
  @Setter
  private String tokenVerificationClass;

  /**
   * <strong>api.tokenValidationEndpoint</strong><br>The url for the identity provider's token validation endpoint
   */
  @Getter
  @Setter
  private String tokenValidationEndpoint;

  /**
   * <strong>api.check-ip-address</strong><br>Check if the IP address in the jwt token matches the ip address of the request
   */
  @Getter
  @Setter
  private Boolean checkIpAddress = true;

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
  private CorsConfig cors;

  /**
   * <strong>api.report-defs</strong><br>Configuration for measures supported by the system
   */
  @Getter
  private ApiReportDefsConfig reportDefs;

  /**
   * <strong>api.measure-packages</strong><br>Configuration for multi measures supported by the system
   */
  @Getter
  private List<ApiMeasurePackage> measurePackages;

  /**
   * <strong>api.user</strong><br>Configuration related to the user that is responsible for running the installation of Link, such as timezone settings.
   */
  @Getter
  private UserConfig user;


  /**
   * The key represents the “type” of data source (csv, excel, etc.) and the value represents the class to use to process the data.
   */
  @Getter
  private HashMap<String, String> dataProcessor;

  /**
   * The string represents the data measure report id that gets continuously updated.
   */
  @Getter
  private String dataMeasureReportId;

  @Getter
  private String reportAggregator;

  @Getter
  private String socketTimeout;

  /**
   * <strong>api.measure-evaluation-threads</strong><br>The number of threads to use for patient measure report generation.
   */
  private Integer measureEvaluationThreads;
}
