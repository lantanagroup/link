package com.lantanagroup.nandina.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
  @Autowired
  private Environment env;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
      .addResourceHandler("/**/*")
      .addResourceLocations(this.env.getProperty("spring.resources.static-locations"), "classpath:/public/")
      .resourceChain(false)
      .addResolver(new PathResourceResolver() {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
          Resource requestedResource = location.createRelative(resourcePath);
          return requestedResource.exists() && requestedResource.isReadable() ? requestedResource : new ClassPathResource("/public/index.html");
        }
      })
      .addTransformer(new MainPageTransformer());
  }
}
