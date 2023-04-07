package com.lantanagroup.link.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class YamlMessageConfiguration extends WebMvcConfigurerAdapter {
  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new YamlMessageConverter());
  }
}
