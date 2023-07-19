package com.lantanagroup.link.config.datagovernance;

import org.junit.Assert;
import org.junit.Test;

public class DataGovernanceConfigTest {

  @Test
  public void getRetentionPeriod() {

    Integer chunkSize = 100;

    DataGovernanceConfig dataGovernanceConfig = new DataGovernanceConfig();
    Assert.assertNull(dataGovernanceConfig.getCensusListRetention());
    Assert.assertNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.assertNull(dataGovernanceConfig.getMeasureReportRetention());
    Assert.assertNull(dataGovernanceConfig.getResourceTypeRetention());
    Assert.assertNull(dataGovernanceConfig.getOtherTypeRetention());
    Assert.assertNull(dataGovernanceConfig.getExpungeRole());
    Assert.assertNull(dataGovernanceConfig.getExpungeChunkSize());

    dataGovernanceConfig.setCensusListRetention("PT4H");
    dataGovernanceConfig.setPatientDataRetention("PT4H");
    dataGovernanceConfig.setMeasureReportRetention("PT4H");
    dataGovernanceConfig.setResourceTypeRetention("PT4H");
    dataGovernanceConfig.setOtherTypeRetention("PT4H");
    dataGovernanceConfig.setExpungeRole("expunge-role");
    dataGovernanceConfig.setExpungeChunkSize(chunkSize);

    Assert.assertNotNull(dataGovernanceConfig.getCensusListRetention());
    Assert.assertNotNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.assertNotNull(dataGovernanceConfig.getMeasureReportRetention());
    Assert.assertNotNull(dataGovernanceConfig.getResourceTypeRetention());
    Assert.assertNotNull(dataGovernanceConfig.getOtherTypeRetention());
    Assert.assertNotNull(dataGovernanceConfig.getExpungeRole());
    Assert.assertNotNull(dataGovernanceConfig.getExpungeChunkSize());
    Assert.assertEquals(chunkSize, dataGovernanceConfig.getExpungeChunkSize());
  }
}