package com.lantanagroup.link.spring;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

public class YamlMessageConverter extends AbstractJackson2HttpMessageConverter {
  protected YamlMessageConverter() {
    // Could use .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE) on YAMLMapper to support kebab
    // cased property names. Can't use a mix of strategies though, so defaulting to the strategy that
    // most closely aligns with the property names defined on the classes
    super(new YAMLMapper(), MediaType.parseMediaType("application/x-yaml"));
  }
}
