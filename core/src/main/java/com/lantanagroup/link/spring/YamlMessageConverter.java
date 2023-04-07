package com.lantanagroup.link.spring;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

public class YamlMessageConverter extends AbstractJackson2HttpMessageConverter {
  protected YamlMessageConverter() {
    super(new YAMLMapper(), MediaType.parseMediaType("application/x-yaml"));
  }
}
