package com.lantanagroup.link.config.datastore;

import com.lantanagroup.link.config.DataSourceConfig;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import com.lantanagroup.link.config.Permission;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "datastore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class DataStoreConfig {
  @Getter
  private DataSourceConfig dataSource;
  @Getter
  private Permission[] permissions;
  @Getter
  private String azorica;

  /***
   <strong>datastore.issuer</strong><br>This issuer is used during token validation to ensure that the JWT has been issued by a trusted system
   */
  @Getter
  private String issuer;
  /***
   <strong>datastore.authJwksUrl</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
   */
  @Getter
  private String authJwksUrl;
}
