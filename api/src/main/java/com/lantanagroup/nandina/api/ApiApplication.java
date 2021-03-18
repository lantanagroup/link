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
import org.springframework.web.filter.DelegatingFilterProxy;

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
}