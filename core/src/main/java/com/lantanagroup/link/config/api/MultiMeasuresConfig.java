package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import java.util.List;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "measure-packages")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)

public class MultiMeasuresConfig {

  List<MultiMeasure> multiMeasureList;

}
