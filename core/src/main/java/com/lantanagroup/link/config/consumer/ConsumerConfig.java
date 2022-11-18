package com.lantanagroup.link.config.consumer;

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
@ConfigurationProperties(prefix = "consumer")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ConsumerConfig {
  @Getter
  private DataSourceConfig dataSource;
  @Getter
  private Permission[] permissions;
  @Getter
  private String azorica;

  /***
   <strong>consumer.issuer</strong><br>This issuer is used during token validation to ensure that the JWT has been issued by a trusted system
   */
  @Getter
  private String issuer;

  /**
   * <strong>consumer.oauth.algorithm</strong><br>The algorithm used by the identity provider to sign the jwt token
   */
  @Getter
  private String algorithm;

  /**
   * <strong>consumer.oauth.tokenVerificationClass</strong><br>The class configured to verify a jwt token in the api
   */
  @Getter
  private String tokenVerificationClass;

  /***
   <strong>consumer.authJwksUrl</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
   */
  @Getter
  private String authJwksUrl;

  /**
   * <strong>consumer.tokenValidationEndpoint</strong><br>The url for the identity provider's token validation endpoint
   */
  @Getter
  private String tokenValidationEndpoint;

}
