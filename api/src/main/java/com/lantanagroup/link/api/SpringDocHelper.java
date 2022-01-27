package com.lantanagroup.link.api;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.reflect.ClassPath;
import org.springdoc.core.SpringDocUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;

public class SpringDocHelper {
  public static void ignoreFhirClasses() {
    SpringDocUtils config = SpringDocUtils.getConfig();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    // Ignore HAPI classes
    try {
      for (final ClassPath.ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
        if (info.getName().startsWith("org.hl7.fhir.r4.model.")) {
          final Class<?> clazz = info.load();
          config.addResponseWrapperToIgnore(clazz);
        }
      }
    } catch (IOException e) {
      // ignore
    }

    config.addResponseWrapperToIgnore(Authentication.class);
    config.addResponseWrapperToIgnore(Claim.class);
    config.addResponseWrapperToIgnore(DecodedJWT.class);
    config.addResponseWrapperToIgnore(GrantedAuthority.class);
  }
}
