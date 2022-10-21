package com.lantanagroup.link.query.auth;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.Assert;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

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
    EpicAuthConfig config = new EpicAuthConfig();
    config.setAudience("http://some-audience.com");
    config.setClientId("some-test");

    KeyPair kp = getKeyPair();
    PrivateKey key = kp.getPrivate();
    config.setKey(Base64.getEncoder().encodeToString(key.getEncoded()));

    String jwtContent = EpicAuth.getJwt(config);

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
    Assert.assertNotNull(body.get("exp"));
    Assert.assertNotEquals((Integer) 0, (Integer) body.get("exp"));
  }

  @Test
  public void testRequestJwt() throws Exception {
    EpicAuthConfig config = new EpicAuthConfig();
    config.setKey("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCgD6vyqHMUK4NFhnc3coOSbZ/pH2Tg8zYrl2ROYXrVJSrf5JRkjk+zOdszf4k17xjCPZvpi42CzVQlpX8/b56c8/mlI5nncl4Loz4WGjx3/KRdIoVaBxr1cY4gzYQZuaHzSUAVnYfgSJ8WhUG2mnwMe5TLsamBEvQoA2T/z4FQf5yhqefOITe3+C5RHDwT0IuSV6GukvlRNbg1Rg8jkkjrSUqD8xyh28RtFrQDz4Pd4VlXsG6AYTdTKqwEjw1fQPOFHb/NGbgv9FhC/PdLqRzqpCeyp+CKQjIUOT+0y8O5I8RE8tMWxvo2EN73r+bcLz31gN7jxjAK8KVxpMZ2t5dBAgMBAAECggEAX6heC/yuIezLXD67evC+P0Gy4wD3KhVQV4b31Hwfi8jVsc8K/HmsmiFGpqVe3FPTiSqGxnG3leeelY0t2jycH5MTrKT1MsQ//laGIXF2mALuPBcIeUBr1SoTVfldLH6rkhlB6mkmLl1Ybn4fQsFax58H0yCPe+tW7Z7xuoxJ7VAUhwWZVM/sH2FGtCX5I5fZlsjN8EKNqVWQ8MmNJ0DxjmigaOyLRIOZttTURgJBuiXjHKBatmQ+8+8Kwktf7uJFWQjONUQvCssvhPQR/1hy3zMsAZP/InabEfp4vtLXEgRVjvek4RUXdX+nnHWc1ivkwRaSyTKEMVSq19bNgoJLLQKBgQDVFbDYiO05uWWVNSFeFjd5PE8K+8c8ITyms4VUO//Uwgtxyb0sml3G7bJ+eV0Wh5YfIazBqp0Th2qflWCGKnNZ698TqxBOWF4kHmDuuCs/fatO7107edkm/N+X2FQGKThFtUTVb2lXMrRrHLokwW/yegZXmQwu0IhrZm8BLf8PgwKBgQDATCxM7D0Gh1M+M8YFUj2d+4gs6TZbS/cyf7QuX3NIxaZNKiewCKZgGT9+vxDWjYUfYif+fguVdtvyXAsKxsyKJS6ZN4togJvyZuAU0MjQKouUxpymFKrCrik2OpqDZ59CicNdbDbQtAgVa7hr3/uY99UlYDjNiPcyUkKKOMEe6wKBgCIrKUF/q3KfLQ/hBim9LEYPiqk7OHaG6d/dV5rrSBnIx+cGL4SQeEsm4IFxWqD7OvJhBv/DKQ7xnwJaBLFe60JXFV1lB+dYOjhWerqs45p5v1eYAH5CCrU6xWvm74pRX8mlyJTlGaI26kFmyN6N+jKKqKuxSgfTvpxiP5iT1JubAoGASgGG0xvf6JFQIhI+1XFvMUvKfq5KMxyrSA4JxAcMESev9uaQW5kYnsdYvmi+DDRu1UMrbTAJOT6DK2TtAvq7YTcqFeFgj5lfawzYlGo9vo+BJILDas9tYHsydSJcsNHCYxMt0tiAyBVhPw0z7qBG7foDV90OP1vE9uLdpOBuwn0CgYEAkV3rDEWd8ndJUEibYqhYyEO9+J2dmAcup9mmrjxMsIybFOPCl0nHtf54A7l+jNayaw6jjut85J6QKtZlks1S5tlUnIBCD/OiZAiUQ9WdMnRq8mDE1I9MmX8jpSi/4B04BGT8V8U6OrnbQ/V6ADdINzhjeiqS65REmY/mDeb764Q=");
    config.setTokenUrl("https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token");
    config.setAudience("https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token");
    config.setClientId("a1923cf4-2e9f-44bc-954d-01ae67350b62");

    EpicAuth auth = new EpicAuth(config);
    String token = auth.getAuthHeader();

    Assert.assertNotNull(token);
    Assert.assertNotEquals(0, token.length());

    System.out.println("Access token is: " + token);
  }
}
