package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import com.lantanagroup.link.config.Permission;
import com.lantanagroup.link.config.Role;
import com.lantanagroup.link.config.consumer.ConsumerConfig;
import com.lantanagroup.link.consumer.auth.AuthInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthInterceptorTest {

  List<Permission> permissions;
  java.util.Map<String, String[]> parameters;
  Role roleDefault;

  @Before
  public void setUp() throws Exception {
    roleDefault = new Role();
    roleDefault.setName("default");
    String[] rights = {"write"};
    roleDefault.setPermission(rights);
    permissions = new ArrayList<>();
  }

  public void setUpBundleConfiguration() {

    //setUp Bundle configuration
    Permission permission = new Permission();
    permission.setResourceType("Bundle");
    List<Role> roles = new ArrayList<>();
    Role role = new Role();
    role.setName("admin");
    String[] rights = {"read", "write", "create"};
    role.setPermission(rights);
    roles.add(role);
    roles.add(roleDefault);
    permission.setRoles(roles.toArray(new Role[roles.size()]));
    permissions.add(permission);
  }

  public void setUpMeasureReportConfiguration() {

    //setUp MeasureReport configuration
    Permission permission = new Permission();
    permission.setResourceType("MeasureReport");
    List<Role> roles = new ArrayList<>();
    Role role = new Role();
    role.setName("admin");
    String[] rights = {"read", "write", "delete"};
    role.setPermission(rights);
    roles.add(role);
    roles.add(roleDefault);
    permission.setRoles(roles.toArray(new Role[roles.size()]));
    permissions.add(permission);
  }

  private void setUpAdmin() throws Exception {
    parameters = new HashMap<>();
    String[] roles = {"admin", "default-roles-nhsnlink", "offline_access", "uma_authorization"};
    parameters.put("roles", roles);
  }

  private void setUpUser() throws Exception {
    parameters = new HashMap<>();
    String[] roles = {"default-roles-nhsnlink", "offline_access", "uma_authorization"};
    parameters.put("roles", roles);
  }


  @Test
  public void testAdminRolesForBundle() throws Exception {
    setUpAdmin();
    setUpBundleConfiguration();
    ConsumerConfig consumerConfig = mock(ConsumerConfig.class);
    when(consumerConfig.getPermissions()).thenReturn(permissions.toArray(new Permission[permissions.size()]));
    RequestDetails requestDetails = mock(RequestDetails.class);
    when(requestDetails.getParameters()).thenReturn(parameters);
    AuthInterceptor interceptor = new AuthInterceptor(consumerConfig);
    List<IAuthRule> list = interceptor.buildRuleList(requestDetails);
    Assert.assertEquals(4, list.size());
  }

  @Test
  public void testAdminRolesForMeasureReport() throws Exception {
    setUpAdmin();
    setUpMeasureReportConfiguration();
    ConsumerConfig consumerConfig = mock(ConsumerConfig.class);
    when(consumerConfig.getPermissions()).thenReturn(permissions.toArray(new Permission[permissions.size()]));
    RequestDetails requestDetails = mock(RequestDetails.class);
    when(requestDetails.getParameters()).thenReturn(parameters);
    AuthInterceptor interceptor = new AuthInterceptor(consumerConfig);
    List<IAuthRule> list = interceptor.buildRuleList(requestDetails);
    Assert.assertEquals(4, list.size());
  }

  @Test
  public void testUserRolesForBundle() throws Exception {
    setUpUser();
    setUpBundleConfiguration();
    ConsumerConfig consumerConfig = mock(ConsumerConfig.class);
    when(consumerConfig.getPermissions()).thenReturn(permissions.toArray(new Permission[permissions.size()]));
    RequestDetails requestDetails = mock(RequestDetails.class);
    when(requestDetails.getParameters()).thenReturn(parameters);
    AuthInterceptor interceptor = new AuthInterceptor(consumerConfig);
    List<IAuthRule> list = interceptor.buildRuleList(requestDetails);
    Assert.assertEquals(2, list.size());
  }


  @Test
  public void testUserRolesForMeasureReport() throws Exception {
    setUpUser();
    setUpMeasureReportConfiguration();
    ConsumerConfig consumerConfig = mock(ConsumerConfig.class);
    when(consumerConfig.getPermissions()).thenReturn(permissions.toArray(new Permission[permissions.size()]));
    RequestDetails requestDetails = mock(RequestDetails.class);
    when(requestDetails.getParameters()).thenReturn(parameters);
    AuthInterceptor interceptor = new AuthInterceptor(consumerConfig);
    List<IAuthRule> list = interceptor.buildRuleList(requestDetails);
    Assert.assertEquals(2, list.size());
  }

}
