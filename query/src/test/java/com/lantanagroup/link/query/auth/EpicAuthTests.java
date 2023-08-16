package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.tenant.FhirQuery;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.model.tenant.auth.EpicAuth;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EpicAuthTests {
  private static KeyPair getKeyPair() throws NoSuchAlgorithmException {
    //https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator
    //KeyPairGenerator only accepts (DSA, RSA, and EC
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }

  @Test
  public void getJwtTest() throws NoSuchAlgorithmException, InvalidKeySpecException {
    EpicAuth config = new EpicAuth();
    config.setAudience("http://some-audience.com");
    config.setClientId("some-test");

    KeyPair kp = getKeyPair();
    PrivateKey key = kp.getPrivate();
    config.setKey(Base64.getEncoder().encodeToString(key.getEncoded()));

    String jwtContent = com.lantanagroup.link.query.auth.EpicAuth.getJwt(config);

    //Date expectedExp = new Date(System.currentTimeMillis() + (1000 * 60 * 4) / 1000);
    Jwt<?, DefaultClaims> jwt = Jwts.parser()
            //.requireExpiration(expectedExp)
            .setSigningKey(kp.getPublic())
            .parse(jwtContent);
    DefaultClaims body = jwt.getBody();

    Assert.assertEquals(body.get("iss"), config.getClientId());
    Assert.assertEquals(body.get("sub"), config.getClientId());
    Assert.assertEquals(body.get("aud"), config.getAudience());
    Assert.assertNotNull(body.get("jti"));
    Assert.assertNotEquals(0, ((String) body.get("jti")).length());
    // Should be four minutes in the future
    Assert.assertEquals((int) (System.currentTimeMillis() / 1000) + (60 * 4), body.get("exp"));
    Assert.assertNotEquals((Integer) 0, (Integer) body.get("exp"));
  }

  @Test
  public void testRequestJwt() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<Object> tokenResponse = mock(HttpResponse.class);
    when(tokenResponse.body()).thenReturn("{ \"access_token\": \"test\" }");
    when(mockClient.send(any(), any())).thenReturn(tokenResponse);

    EpicAuth epicAuthConfig = new EpicAuth();

    // Fake key generated just for this unit test
    epicAuthConfig.setKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOSDVPprAuGfTdFgYwppoX+wMY30Kz3N6fMFghXCsf7M4tkWXLTMnmsw5L1/o6s55JU994CqZ4XMuMWxnGeuKvg3Bk1J+wrgoz1Veb3o7RTgKvIgis0TlrJLGpjdz5QooUDD/jdfgt0sCg4+6OnKIiXcG+jFf8NiZEP5aR2AdzVwW7vnQ8wNsElvJ3VPzJGfx/m1sw4oClKmrf8sNat4S2XFngVXI5ztIZ0rGvQoKC19ZT46LFcvfgsxAyl27Y67iwgc1Wd7oWJleJYStsoP9bPSDuQhtvJNxZeMF7hSEZoi6FogejkEP6IgtVaiOmWRKKL8X4LrrTwT+YAC9/B3t5AgMBAAECggEAEKr9/8cgwkDfq8OdUW4M0nY6XFah7aWhqWw/Gsb3Rxeb20u91W0JfWYneBJmCjs8RYGM84asx9n7qoYr8YE5cuQkf0k89ApEEcch0prroFb02ZDe8ZPgV1t6LZCkc9GgFZmdyfPcz1sRrI5uVsCqSyuXPZ3aeCsp8WbP9E3n4DZZIk6jSbxMWh+Ljych3moqIqgA5oEjhITzWpw+4+jsaL2ZQ8Q2jIc67dL7gGj62j9xmM9df41I5XFtaxXgwlI8jAeknm1ZBtt5veKkBemeqHbIPdZTUTgh/ldOOIUbMkX3c1zOvm8nQfn2oUA2yEDKEEYEZYu5PuJFmBk5fhZ5gQKBgQD5PFUESXjSJoTkq+hJoQsthkbwufbKZMeP2Oje72HHJfXEt23bwvEmXQhfG++ANwxeHkYrXGubRY2wlyyvvniY9CGtaY6dpYkM+tlKcbsrX8jf/60oM+ZTIhFAjQKVDdQOAJw+A1zWZNVXRTb37i8I/CWrMG3yEO8YtTlsUCen4QKBgQDT4XAtMeGdq7nwqI9ihKg47RefNM9VY4hyxeRIC+o8C6BgWgWqfaUD1v8Y6FYQ6Kc+ecfMybNw3/h+ZqLngiU/97s+c1qZLc3rYFIT28INBdnJvJeNnMdP4gpYBJAMZwU6Jk5q7zjuDFuKHYzSNsuJJylsL84vb6uTRudExoDmmQKBgQCds6snt0WX03+rIYsta5UjDR++Gi0qC3KqdlmDFn0MAdzsyBPfRg5Ic/1kEM0Ol8Zfl1BXB3efG/d3kXBZE3BDd9YHYA85eRCrAd6T2DrSSx1TfvgVxCEs4RWBfrfvkHxpD8HNC1um3M6RFd6eKjvgt4suMsqerFBdle3rtxwggQKBgGe9gYcfXTAt3KSQY2QBP7xfmalB2iIc0bogWl1MOCueJbAHY67ORfVTVyOC3mD5CKn2RnVmbI1fm/OHnUts1YA0c2FoaXuGicQtLQK8Ho7xxmiYBjw3/v6F1jqyQTVRW6XVC6Af8Ofc9RTy0vg6C/3jRszJu1JOgtthY+qwpnxBAoGBAOawtKtKR9wN+VJvhJxks8fPct9ShDa9YocMvytv4qgBD2ma2+ec/8kyB2qzpOiOfF6HCbeRCfe63LH71oYWr5MeIisnfuts8EJuNYoPKusk/1cACJtZ+T+XH4hcMYvDhs7cTxG3RWCehW10ReQk/jJY9h3eD4x05vWlGkKQyuhc");
    epicAuthConfig.setTokenUrl("http://test.com/auth/token");
    epicAuthConfig.setAudience("http://some-audience.com");
    epicAuthConfig.setClientId("some-fake-client-id");

    com.lantanagroup.link.query.auth.EpicAuth auth = new com.lantanagroup.link.query.auth.EpicAuth();
    auth.setClient(mockClient);

    TenantService tenantService = mock(TenantService.class);
    Tenant tenant = new Tenant();
    tenant.setFhirQuery(new FhirQuery());
    tenant.getFhirQuery().setEpicAuth(epicAuthConfig);
    when(tenantService.getConfig()).thenReturn(tenant);

    auth.setTenantService(tenantService);

    String token = auth.getAuthHeader();

    Assert.assertNotNull(token);
    Assert.assertEquals("Bearer test", token);

    ArgumentCaptor<HttpRequest> requestArg = ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockClient).send(requestArg.capture(), any());
    Assert.assertNotNull(requestArg.getValue());
    Assert.assertEquals("POST", requestArg.getValue().method());
    Assert.assertEquals("http://test.com/auth/token", requestArg.getValue().uri().toString());
  }
}
