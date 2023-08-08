package com.lantanagroup.link;

import org.junit.Assert;
import org.junit.Test;

public class HelperTests {
  /**
   * Correctly formatted connection string with a "database" property should return correct value
   */
  @Test
  public void getDatabaseNameTest1() {
    String connectionString = "jdbc:sqlserver://some-host:1433;database=myDb1;user=some-user;password=some-pass;encrypt=true";
    String databaseName = Helper.getDatabaseName(connectionString);
    Assert.assertEquals("myDb1", databaseName);
  }

  /**
   * Correctly formatted connection string with a "databaseName" property (alias for "database") should return correct value
   */
  @Test
  public void getDatabaseNameTest2() {
    String connectionString = "jdbc:sqlserver://some-host:1433;user=some-user;password=some-pass;encrypt=true;databaseName=myDb2";
    String databaseName = Helper.getDatabaseName(connectionString);
    Assert.assertEquals("myDb2", databaseName);
  }

  /**
   * Correctly formatted connection string with a "database" property in a different position should return correct value
   */
  @Test
  public void getDatabaseNameTest3() {
    String connectionString = "jdbc:sqlserver://some-host:1433;user=some-user;password=some-pass;encrypt=true;database=myDb3";
    String databaseName = Helper.getDatabaseName(connectionString);
    Assert.assertEquals("myDb3", databaseName);
  }

  /**
   * Incorrectly formatted connection string should return null
   */
  @Test
  public void getDatabaseNameTest4() {
    String connectionString = "jdbc:sqlserver://som";
    String databaseName = Helper.getDatabaseName(connectionString);
    Assert.assertNull(databaseName);
  }

  /**
   * Correctly formatted connection string without a "database" property should return null
   */
  @Test
  public void getDatabaseNameTest5() {
    String connectionString = "jdbc:sqlserver://some-host:1234;db=asdff";
    String databaseName = Helper.getDatabaseName(connectionString);
    Assert.assertNull(databaseName);
  }
}
