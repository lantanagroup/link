package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "query.auth.token")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class TokenAuthConfig {
  private String token;
}
