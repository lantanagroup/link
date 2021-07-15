package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.model.UserModel;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  @GetMapping
  public List<UserModel> getUsers (Authentication authentication, HttpServletRequest request) throws Exception {

    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    List<UserModel> users = new ArrayList<>();

    Bundle bundle = fhirStoreClient
            .search()
            .forResource(Practitioner.class)
            .withTag(Constants.MainSystem, Constants.LinkUserTag)
            .returnBundle(Bundle.class)
            .execute();

    if (bundle.getEntry().size() == 0) {
      logger.info("No practitioner ");
      return users;
    }

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, fhirStoreClient, ctx);
    Stream<UserModel> lst = bundles.parallelStream().map(practitioner -> new UserModel((Practitioner) practitioner));

    return lst.collect(Collectors.toList());
  }
}
