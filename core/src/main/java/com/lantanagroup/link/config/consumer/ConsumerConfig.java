package com.lantanagroup.link.config.consumer;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
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
  /***
   <strong>consumer.authJwksUrl</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
   */
  @Getter
  private String authJwksUrl;
}
