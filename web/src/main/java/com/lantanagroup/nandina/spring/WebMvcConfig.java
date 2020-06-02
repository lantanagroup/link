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
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
  @Autowired
  private Environment env;

  private String[] getResourceLocations() {
    List<String> resourceLocations = new ArrayList();

    if (this.env.getProperty("spring.resources.static-locations") != null) {
      resourceLocations.add(this.env.getProperty("spring.resources.static-locations"));
    }

    resourceLocations.add("classpath:/public/");

    return resourceLocations.toArray(new String[] { });
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
      .addResourceHandler("/**/*")
      .addResourceLocations(this.getResourceLocations())
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
