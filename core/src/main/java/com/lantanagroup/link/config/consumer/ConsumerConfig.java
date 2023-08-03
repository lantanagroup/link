package com.lantanagroup.link.config.consumer;

import com.lantanagroup.link.config.DataSourceConfig;
import com.lantanagroup.link.config.YamlPropertySourceFactory;
import com.lantanagroup.link.config.Permission;
import com.lantanagroup.link.config.auth.LinkAuthManager;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "consumer")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ConsumerConfig {
  @NotNull
  private DataSourceConfig dataSource;
  @NotNull
  private Permission[] permissions;
  private String azorica; // WHAT is this for?
  @NotNull
  private LinkAuthManager linkAuthManager;
}
