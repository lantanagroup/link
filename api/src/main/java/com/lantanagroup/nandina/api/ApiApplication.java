package com.lantanagroup.nandina.api;

import com.lantanagroup.nandina.api.config.ApiConfig;
import com.lantanagroup.nandina.api.config.ApiQueryConfigModes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SpringBootApplication
public class ApiApplication extends SpringBootServletInitializer implements InitializingBean {
  @Autowired
  private ApplicationContext context;

  @Autowired
  private ApiConfig config;

  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
      if (StringUtils.isEmpty(this.config.getQuery().getUrl())) {
        throw new Exception("When query.mode is \"Remote\", query.url is required");
      }
      if (StringUtils.isEmpty(this.config.getQuery().getApiKey())) {
        throw new Exception("When query.mode is \"Remote\", query.apiKey is required");
      }
    }
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    servletContext
            .addFilter("securityFilter", new DelegatingFilterProxy("springSecurityFilterChain"))
            .addMappingForUrlPatterns(null, false, "/*");

    super.onStartup(servletContext);
  }


  /**
   * This method is responsible for setting the CORS configuration based on the api.yml (or its override)
   * @return WebMvcConfigurer
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    // TODO: Replace with configuration
    String allowsOrigins = this.config.getCors().getAllowedOrigins();
    String[] allowedMethods = this.config.getCors().getAllowedMethods();
    String allowedHeaders = this.config.getCors().getAllowedHeaders();
    Boolean allowCredentials = this.config.getCors().getAllowedCredentials();

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOrigins(allowsOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials);
      }
    };
  }
}