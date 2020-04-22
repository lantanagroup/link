package com.lantanagroup.flintlock;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/main.js")
      .addResourceLocations("classpath:/public/main.js")
      .resourceChain(false)
      .addTransformer(new MainPageTransformer());
  }
}
