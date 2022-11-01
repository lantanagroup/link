package com.lantanagroup.link.config.datagovernance;

import org.junit.Assert;
import org.junit.Test;

public class DataGovernanceConfigTest {

  @Test
  public void getRetentionPeriod() {
    DataGovernanceConfig dataGovernanceConfig = new DataGovernanceConfig();
    Assert.assertNull(dataGovernanceConfig.getCensusListRetention());
    Assert.assertNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.assertNull(dataGovernanceConfig.getReportRetention());

    dataGovernanceConfig.setCensusListRetention("PT4H");
    dataGovernanceConfig.setPatientDataRetention("PT4H");
    dataGovernanceConfig.setReportRetention("PT4H");

    Assert.assertNotNull(dataGovernanceConfig.getCensusListRetention());
    Assert.assertNotNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.assertNotNull(dataGovernanceConfig.getReportRetention());
  }
}