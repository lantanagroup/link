package com.lantanagroup.link.cache;

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
@ConfigurationProperties(prefix = "cache.azure")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class AzureCacheForRedisConfig {
  private String hostname;
  private String key;
  private Boolean useSsl = true;
  private Integer port = 6380;
}
