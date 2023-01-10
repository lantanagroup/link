package com.lantanagroup.link.nhsn;


import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "applyconceptmaps")
@PropertySource(value = {"classpath:application.yml", "classpath:application-site.yml"}, factory = YamlPropertySourceFactory.class)
public class ApplyConceptMapsConfig {
  private List<ApplyConceptMapConfig> conceptMaps;
}

