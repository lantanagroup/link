package com.lantanagroup.link.agent.auth;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.*;

public class AgentAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  protected static final Logger logger = LoggerFactory.getLogger(AgentAuthFilter.class);
  private static Dictionary<String, String> hostNameIPs = new Hashtable<>();

  private static String getIPAddress(String value) {
    if (AgentAuthFilter.hostNameIPs.get(value) != null) {
      return AgentAuthFilter.hostNameIPs.get(value);
    }

    Pattern ip4Pattern = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$", Pattern.MULTILINE);
    Pattern ip6Pattern = Pattern.compile("\\\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\b", Pattern.MULTILINE);

    if (ip4Pattern.matcher(value).matches() || ip6Pattern.matcher(value).matches()) {
      return value;
    }

    try {
      String next = InetAddress.getByName(value).toString();

      if (next.indexOf("/") == 0) {
        next = next.substring(next.lastIndexOf("/") + 1);
      }

      logger.debug(String.format("Resolved host name \"%s\" as IP \"%s\"", value, next));

      AgentAuthFilter.hostNameIPs.put(value, next);

      return next;
    } catch(UnknownHostException e) {
      logger.error("Cannot resolve host name \"" + value + "\" in white-list: " + e.getMessage());
    }

    return value;
  }

  public AgentAuthFilter(String expectedApiKey, String[] allowedRemotes, String proxyAddress) {
    this.setAuthenticationManager(new AuthenticationManager() {
      @Override
      public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AgentAuthModel authModel = (AgentAuthModel) authentication.getPrincipal();

        //Initially had this running a check on the IP for properly formatted addresses but running the address through
        //the InetAddress.getByName method requires a try/catch block. So, it's effectively the same to just run each
        //through the method and let the catch block notify the user of incorrectly formatted IP addresses. (Error will
        //only show in the logs.)
        for (int x = 0; x < allowedRemotes.length; x++){
          allowedRemotes[x] = AgentAuthFilter.getIPAddress(allowedRemotes[x].trim());
        }

        if (!expectedApiKey.equals(authModel.getAuthorization())) {
          String msg = String.format("The API Key \"%s\" was not found or not the expected value.", authModel.getAuthorization());
          logger.error(msg);
          throw new BadCredentialsException(msg);
        } else {
          logger.debug("The API key in the request matches configuration");
        }

        if (!Strings.isNullOrEmpty(proxyAddress)) {
          String realProxyAddress = AgentAuthFilter.getIPAddress(proxyAddress.trim());

          if (!realProxyAddress.equals(authModel.getRemoteAddress())) {
            String msg = String.format("The remote address \"%s\" did not match the proxy address \"%s\"", authModel.getRemoteAddress(), realProxyAddress);
            logger.error(msg);
            throw new BadCredentialsException(msg);
          } else {
            logger.debug(String.format("The remote address \"%s\" matches the proxy address", authModel.getRemoteAddress()));
          }

          if (Arrays.asList(allowedRemotes).indexOf(authModel.getForwardedRemoteAddress()) < 0) {
            String msg = String.format("The remote address \"%s\" was not in the configured allowed forwarded (proxied) addresses", authModel.getForwardedRemoteAddress());
            logger.error(msg);
            throw new BadCredentialsException(msg);
          } else {
            logger.debug(String.format("The addresses forwarded by the proxy server (%s) was found in the allowed list", authModel.getForwardedRemoteAddress()));
          }
        } else if (Arrays.asList(allowedRemotes).indexOf(authModel.getRemoteAddress()) < 0) {
          String msg = String.format("The remote address \"%s\" was not in the configured allowed remote addresses", authModel.getRemoteAddress());
          logger.error(msg);
          throw new BadCredentialsException(msg);
        } else {
          logger.debug(String.format("Remote address \"%s\" was found in the allowed list.", authModel.getRemoteAddress()));
        }

        authentication.setAuthenticated(true);
        return authentication;
      }
    });
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    AgentAuthModel authModel = new AgentAuthModel();
    String authorization = request.getHeader("Authorization");

    if (authorization != null && authorization.toLowerCase(Locale.ENGLISH).startsWith("key ")) {
      authModel.setAuthorization(authorization.substring(4));
    }

    String xForwardedFor = request.getHeader("x-forwarded-for");
    String xRealIp = request.getHeader("x-real-ip");

    if (!Strings.isNullOrEmpty(xForwardedFor)) {
      authModel.setForwardedRemoteAddress(xForwardedFor);
    } else if (!Strings.isNullOrEmpty(xRealIp)) {
      authModel.setForwardedRemoteAddress(xRealIp);
    }

    authModel.setRemoteAddress(request.getRemoteAddr());

    return authModel;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }
}
