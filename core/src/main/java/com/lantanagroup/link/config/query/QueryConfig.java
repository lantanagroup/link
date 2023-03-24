package com.lantanagroup.link.config.query;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "query")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
/**
 * See the Link WIKI - <a href="https://github.com/lantanagroup/link/wiki/Configuration#query-api">Configuration &gt; Query-API</a>
 */
public class QueryConfig {

  /**
   * <strong>query.require-https</strong><br>Indicates if HTTPS is required for query url.
   */
  @Getter
  private boolean requireHttps;

  /**
   * <strong>query.query-class</strong><br>The class to use for performing the queries.
   */
  @NotBlank
  private String queryClass;

  /**
   * <strong>query.auth-class</strong><br>The class that should be used (if any) to authenticate queries to the specified <strong>query.fhir-server-base</strong>.
   */
  private String authClass;
}
