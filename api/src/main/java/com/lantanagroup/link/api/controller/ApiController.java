package com.lantanagroup.link.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import com.google.common.base.Strings;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.SwaggerConfig;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiInfoGroup;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.model.ApiVersionModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
  @Autowired
  private ApiConfig apiConfig;

  @Autowired
  private SwaggerConfig swaggerConfig;

  @GetMapping(value = "/$tenant-schema", produces = {"text/plain"})
  public String getTenantJsonSchema() {
    SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    SchemaGeneratorConfig config = configBuilder.build();
    SchemaGenerator generator = new SchemaGenerator(config);
    JsonNode jsonSchema = generator.generateSchema(Tenant.class);
    return jsonSchema.toPrettyString();
  }

  @GetMapping
  public ApiVersionModel getVersionInfo() {
    return Helper.getVersionInfo();
  }

  @GetMapping("/info")
  public List<ApiInfoGroup> getInfo() {
    return this.apiConfig.getInfoGroups();
  }

  private static Map<Object, Object> getYamlObj(Map<Object, Object> obj, String property) {
    return (Map<Object, Object>) obj.get(property);
  }

  private static List<LinkedHashMap> getYamlArray(Map<Object, Object> obj, String property) {
    return (List<LinkedHashMap>) obj.get(property);
  }

  private void updateSwaggerSpec(Map<Object, Object> spec, HttpServletRequest request) {
    // Security
    Map<Object, Object> components = getYamlObj(spec, "components");
    Map<Object, Object> securitySchemes = getYamlObj(components, "securitySchemes");

    if (StringUtils.isNotEmpty(this.swaggerConfig.getAuthUrl()) && StringUtils.isNotEmpty(this.swaggerConfig.getTokenUrl())) {
      Map<Object, Object> oauth = getYamlObj(securitySchemes, "oauth");
      Map<Object, Object> flows = getYamlObj(oauth, "flows");
      Map<Object, Object> authorizationCode = getYamlObj(flows, "authorizationCode");
      LinkedHashMap<String, String> scopeObj = new LinkedHashMap<>();
      authorizationCode.put("scopes", scopeObj);

      if (this.swaggerConfig.getScope() != null) {
        for (String scope : this.swaggerConfig.getScope()) {
          scopeObj.put(scope, scope);
        }
      }

      if (StringUtils.isNotEmpty(this.swaggerConfig.getAuthUrl())) {
        authorizationCode.put("authorizationUrl", this.swaggerConfig.getAuthUrl());
      }

      if (StringUtils.isNotEmpty(this.swaggerConfig.getTokenUrl())) {
        authorizationCode.put("tokenUrl", this.swaggerConfig.getTokenUrl());
      }
    } else {
      securitySchemes.remove("oauth");
      List security = getYamlArray(spec, "security");
      security.remove(0);
    }

    // Servers
    List<LinkedHashMap> servers = getYamlArray(spec, "servers");
    for (LinkedHashMap server : servers) {
      server.put("url", this.getPublicAddress(request));
    }

    // Version
    ApiVersionModel apiVersionModel = this.getVersionInfo();
    Map<Object, Object> info = getYamlObj(spec, "info");
    info.put("version", apiVersionModel != null && !Strings.isNullOrEmpty(apiVersionModel.getVersion()) ? apiVersionModel.getVersion() : "dev");
  }

  @GetMapping(value = "/docs", produces = "text/yaml")
  public String getDocs(HttpServletRequest request) throws IOException {
    try (InputStream specStream = this.getClass().getClassLoader().getResourceAsStream("swagger.yml")) {
      Yaml yaml = new Yaml();
      Map<Object, Object> spec = yaml.load(specStream);
      this.updateSwaggerSpec(spec, request);
      String content = yaml.dump(spec);
      return content;
    }
  }

  private String getPublicAddress(HttpServletRequest request) {
    if (Strings.isNullOrEmpty(this.apiConfig.getPublicAddress())) {
      return request.getRequestURL().toString().replace("/api/docs", "/");
    }

    if (this.apiConfig.getPublicAddress().endsWith("/")) {
      return this.apiConfig.getPublicAddress()
              .substring(0, this.apiConfig.getPublicAddress().length() - 1)
              .replace("/api", "");
    }

    return this.apiConfig.getPublicAddress()
            .replace("/api", "");
  }
}
