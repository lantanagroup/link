package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.lantanagroup.link.model.UserModel;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public List<UserModel> getUsers (Authentication authentication, HttpServletRequest request) throws Exception {

    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    List<UserModel> users = new ArrayList<>();
    List<IBaseResource> bundles = new ArrayList<>();

    Bundle bundle = fhirStoreClient
            .search()
            .forResource(Practitioner.class)
            .returnBundle(Bundle.class)
            .execute();


    if (bundle.getEntry().size() == 0) {
      logger.info("No practitioner ");
      return users;
    }

    bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

    // Load the subsequent pages
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = fhirStoreClient
              .loadPage()
              .next(bundle)
              .execute();
      logger.info("Adding next page of bundles...");
      bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
    }
    Stream<UserModel> lst = bundles.parallelStream().map(UserModel::new);
    return lst.collect(Collectors.toList());
  }
}
