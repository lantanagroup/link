package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
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
   * <strong>api.name</strong><br>The name of the application that's returned in Device instances of reports
   */
  private String name;

  /**
   * <strong>api.connection-string</strong><br>The connection string to use for tenant-agnostic data
   */
  private String connectionString;

  /**
   * <strong>api.apply-schemas</strong><br>Whether to apply schemas to the databases or not
   */
  private boolean applySchemas = false;

  /**
   * <strong>api.validate-fhir-server</strong><br>Boolean for whether to check for metadata before request or not
   */
  @Getter
  private Boolean validateFhirServer;

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
   * <strong>api.sender</strong><br>The class used to send reports
   */
  @NotNull
  private String sender;

  /**
   * <strong>api.patient-id-resolver</strong><br>The class used to determine the list of patient ids that should be queried for
   */
  private String patientIdResolver;

  /**
   * <strong>api.cors</strong><br>CORS configuration used for browser interaction with the API
   */
  @Getter
  private CorsConfig cors;

  @Getter
  @NotNull
  private String reportAggregator;

  @Getter
  private String socketTimeout;

  @Getter
  @NotNull
  private String debugPath;

  /**
   * <strong>api.measure-evaluation-threads</strong><br>The number of threads to use for patient measure report generation.
   */
  private Integer measureEvaluationThreads;

  /**
   * <strong>api.skip-query</strong><br>Whether to skip the query phase of report generation; useful if patient data bundles have already been stored.
   */
  private boolean skipQuery = false;

  /**
   * <strong>api.no-scheduling</strong><br>When true, won't initialize the scheduling system. Primarily used for debugging purposes.
   */
  private boolean noScheduling = false;

  /**
   * <strong>api.measure-def-urls</strong><br>A set of URLs representing the latest measure definition, keyed by measure ID
   */
  private HashMap<String, String> measureDefUrls = new HashMap<>();

  /**
   * Allows use of QA debugging endpoints. DO NOT ALLOW IN PRODUCTION!!!!
   */
  private boolean allowQaEndpoints = false;

  private List<ApiInfoGroup> infoGroups = new ArrayList<>();

}
