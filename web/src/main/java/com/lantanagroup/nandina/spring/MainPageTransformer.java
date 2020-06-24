package com.lantanagroup.nandina.spring;

import com.lantanagroup.nandina.JsonProperties;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class is responsible for replacing configurable values in the status html pages, such as %auth.issuer%
 */
public class MainPageTransformer implements ResourceTransformer {

  private JsonProperties jsonProperties;

  public MainPageTransformer(JsonProperties properties) {
    jsonProperties = properties;
  }

  @Override
  public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
    String html = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());

    /*
    if (Config.getInstance().googleApiKey != null && !Config.getInstance().googleApiKey.isEmpty()) {
      html = html.replace("%google.api.key%", Config.getInstance().googleApiKey);
    }
     */

    html = html.replace("%auth.issuer%", jsonProperties.getAuthIssuer());
    html = html.replace("%auth.clientId%", jsonProperties.getAuthClientId());
    html = html.replace("%auth.scope%", jsonProperties.getAuthScope());
    html = html.replace("%remember.fields%", jsonProperties.getRememberFields());

    return new TransformedResource(resource, html.getBytes());
  }
}
