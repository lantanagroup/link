package com.lantanagroup.link.config.DataGovernance;

import org.junit.Test;
import org.springframework.util.Assert;

import static org.junit.Assert.*;

public class DataGovernanceConfigTest {

  @Test
  public void getRetentionPeriod() {
    DataGovernanceConfig dataGovernanceConfig = new DataGovernanceConfig();
    RetentionPeriod retentionPeriod = dataGovernanceConfig.getRetentionPeriod();
    Assert.notNull(retentionPeriod);
    Assert.isNull(retentionPeriod.getCensusListRetention());
    Assert.isNull(retentionPeriod.getPatientDataRetention());
    Assert.isNull(retentionPeriod.getReportRetention());

    retentionPeriod.setCensusListRetention("PT4H");
    retentionPeriod.setPatientDataRetention("PT4H");
    retentionPeriod.setReportRetention("PT4H");

    Assert.notNull(retentionPeriod.getCensusListRetention());
    Assert.notNull(retentionPeriod.getPatientDataRetention());
    Assert.notNull(retentionPeriod.getReportRetention());
  }
}