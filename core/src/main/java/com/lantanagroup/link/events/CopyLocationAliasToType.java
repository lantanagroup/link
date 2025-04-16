package com.lantanagroup.link.events;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.ApplyConceptMaps;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CopyLocationAliasToType implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(CopyLocationAliasToType.class);

  private static boolean anyAliasExistsInType(Location location) {
    return location.getType().stream()
            .flatMap(type -> type.getCoding().stream())
            .anyMatch(coding -> StringUtils.equals(coding.getSystem(), Constants.LocationAliasCodeSystem));
  }

  private static boolean existsInType(Location location, StringType alias) {
    return location.getType().stream()
            .flatMap(type -> type.getCoding().stream())
            .anyMatch(coding -> coding.is(Constants.LocationAliasCodeSystem, alias.asStringValue())
                    || ApplyConceptMaps.isMapped(coding, Constants.LocationAliasCodeSystem, alias.asStringValue()));
  }

  private static void copyToType(Location location, StringType alias) {
    location.addType().addCoding()
            .setSystem(Constants.LocationAliasCodeSystem)
            .setCode(alias.asStringValue());
  }

  private final boolean iterative;

  public CopyLocationAliasToType(boolean iterative) {
    this.iterative = iterative;
  }

  public CopyLocationAliasToType() {
    this(false);
  }

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      if (!(resource instanceof Location)) {
        continue;
      }
      Location location = (Location) resource;
      context.putResourceIfAbsent(location);
      if (anyAliasExistsInType(location)) {
        continue;
      }
      Location ancestor = location;
      IGenericClient client = context.getClient();
      while (true) {
        for (StringType alias : ancestor.getAlias()) {
          if (!existsInType(location, alias)) {
            copyToType(location, alias);
          }
        }
        if (!iterative || client == null) {
          break;
        }
        IIdType ancestorId = ancestor.getPartOf().getReferenceElement();
        if (ancestorId == null || !ancestorId.hasIdPart()) {
          break;
        }
        ancestor = context.computeResourceIfAbsent(Location.class, ancestorId, () -> {
          try {
            return client.read()
                    .resource(Location.class)
                    .withId(ancestorId)
                    .execute();
          } catch (Exception e) {
            logger.error("Failed to retrieve ancestor: {}", ancestorId, e);
            return null;
          }
        });
        if (ancestor == null) {
          break;
        }
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
