package com.lantanagroup.link.config.datagovernance;

import org.junit.Test;
import org.springframework.util.Assert;

public class DataGovernanceConfigTest {

  @Test
  public void getRetentionPeriod() {
    DataGovernanceConfig dataGovernanceConfig = new DataGovernanceConfig();
    Assert.isNull(dataGovernanceConfig.getCensusListRetention());
    Assert.isNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.isNull(dataGovernanceConfig.getReportRetention());

    dataGovernanceConfig.setCensusListRetention("PT4H");
    dataGovernanceConfig.setPatientDataRetention("PT4H");
    dataGovernanceConfig.setReportRetention("PT4H");

    Assert.notNull(dataGovernanceConfig.getCensusListRetention());
    Assert.notNull(dataGovernanceConfig.getPatientDataRetention());
    Assert.notNull(dataGovernanceConfig.getReportRetention());
  }
}