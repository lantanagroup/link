package com.lantanagroup.flintlock;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;

public class MainPageTransformer implements ResourceTransformer {
  @Override
  public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
    String html = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());

    /*
    if (Config.getInstance().googleApiKey != null && !Config.getInstance().googleApiKey.isEmpty()) {
      html = html.replace("%google.api.key%", Config.getInstance().googleApiKey);
    }
     */

    return new TransformedResource(resource, html.getBytes());
  }
}
