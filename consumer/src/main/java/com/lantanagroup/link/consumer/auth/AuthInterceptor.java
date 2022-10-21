package com.lantanagroup.link.consumer.auth;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.Permission;
import com.lantanagroup.link.config.Role;
import com.lantanagroup.link.config.consumer.ConsumerConfig;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AuthInterceptor extends AuthorizationInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
  private static final String defaultRole = "default";
  private ConsumerConfig consumerConfig;

  public AuthInterceptor(ConsumerConfig consumerConfig) {
    this.consumerConfig = consumerConfig;
  }

  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

    RuleBuilder ruleBuilder = new RuleBuilder();
    ruleBuilder.allow().metadata();

    // readConsumerConfig
    Permission[] permissions = consumerConfig.getPermissions();
    String[] jwtRoles = theRequestDetails.getParameters().get(Constants.Roles);
    Set<String> userRoleSet = jwtRoles != null ? Arrays.stream(jwtRoles).collect(Collectors.toSet()) : new HashSet<>();

    String azorica = consumerConfig.getAzorica();
    if (azorica != null) {
      logger.info("Azorica " + azorica + " is found.");
    } else {
      logger.info("Azorica not found.");
    }
    if (permissions == null) {
      logger.info("No Permissions set in the configuration file.");
      return ruleBuilder.build();
    }
    for (Permission permission : permissions) {
      String resourceType = permission.getResourceType();
      Role appliedRole = null;
      try {
        // validate Fhir resource type
        Class<? extends IBaseResource> resourceClass = Class.forName(Constants.FhirResourcesPackageName + resourceType).asSubclass(IBaseResource.class);
        if (permission.getRoles() != null) {
          Optional<Role> userRole = Arrays.stream(permission.getRoles()).filter(role -> !defaultRole.equals(role.getName()) && userRoleSet.contains(role.getName())).collect(Collectors.toList()).stream().findFirst();
          if (userRole.isPresent()) {
            appliedRole = userRole.get();
          }
          // if there is no match then apply the default role if exists
          if (appliedRole == null) {
            Optional<Role> defRole = Arrays.stream(permission.getRoles()).filter(role -> defaultRole.equals(role.getName())).collect(Collectors.toList()).stream().findFirst();
            if (defRole.isPresent()) {
              appliedRole = defRole.get();
            }
          }

          // get all the permissions for each role
          if (appliedRole != null) {
            for (String perm : appliedRole.getPermission()) {
              if (perm.startsWith("$")) {
                ruleBuilder.allow().operation().named(perm).onType(resourceClass).andAllowAllResponses().andThen();
                ruleBuilder.allow().operation().named(perm).onInstancesOfType(resourceClass).andAllowAllResponses().andThen();
                continue;
              }
              switch (perm.toLowerCase(Locale.ENGLISH)) {
                case "read":
                  ruleBuilder.allow().read().resourcesOfType(resourceType).withAnyId().andThen();
                  break;
                case "write":
                  ruleBuilder.allow().write().resourcesOfType(resourceType).withAnyId().andThen();
                  break;
                case "delete":
                  ruleBuilder.allow().delete().resourcesOfType(resourceType).withAnyId().andThen();
                  break;
                case "create":
                  ruleBuilder.allow().create().resourcesOfType(resourceType).withAnyId().andThen();
                  break;
                default:
              }
            }
          }
        }
      } catch (Exception ex) {
        logger.error("Error in configuration file. Resource type: " + resourceType + " does not exist.");
      }
    }
    return ruleBuilder.build();
  }
}
