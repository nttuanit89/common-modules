package com.learning.common.restapi;

import static com.google.common.base.Strings.nullToEmpty;
import com.google.common.collect.ImmutableMap;
import com.learning.common.logging.BaseLoggerFactory;
import com.learning.common.security.BaseUser;
import static com.learning.common.security.BaseUser.USER_ATTR_HOST;
import static com.learning.common.security.BaseUser.USER_ATTR_REQUEST_ID;
import static com.learning.common.security.BaseUser.USER_ATTR_TENANT_CONTEXT;
import static com.learning.common.security.BaseUser.USER_ATTR_TENANT_TOKEN;
import java.io.IOException;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.MDC;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
  private static final Logger LOG = BaseLoggerFactory.getLogger(JwtRequestFilter.class);
  private static final int JWT_PARTS = 3;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String jwtToken = nullToEmpty(request.getHeader(AUTHORIZATION));
    String idHeader = "X-REQUEST-ID";
    String requestId = ofNullable(request.getHeader(idHeader)).orElseGet(this::getTraceId);
    response.addHeader(idHeader, requestId);
    MDC.put(USER_ATTR_REQUEST_ID, requestId);

    if (jwtToken.split("\\.", JWT_PARTS).length < JWT_PARTS || !jwtToken.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    String host = nullToEmpty(request.getHeader("Host"));
    ImmutableMap.Builder<String, Object> attributes = ImmutableMap.builder();
    attributes.put(USER_ATTR_REQUEST_ID, requestId);
    attributes.put(USER_ATTR_HOST, host);

    try {
      BaseUser user =
          new BaseUser(
              jwtToken.substring(7),
              singletonList(new SimpleGrantedAuthority("ROLE_Default")),
              attributes.build(),
              USER_ATTR_REQUEST_ID,
              extractHeaders(request));
      OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, emptySet(), "finbase");
      SecurityContextHolder.getContext().setAuthentication(token);
    } catch (Exception ex) {
      LOG.error("Error when parsing JWT: " + ex.getMessage(), ex);
    }
    chain.doFilter(request, response);
  }

  private String getTraceId() {
    return ofNullable(MDC.get("trace_id")).orElseGet(() -> UUID.randomUUID().toString());
  }

  private Map<String, String> extractHeaders(HttpServletRequest request) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put(USER_ATTR_TENANT_CONTEXT, nullToEmpty(request.getHeader("tenant")));
    builder.put(USER_ATTR_TENANT_TOKEN, nullToEmpty(request.getHeader(USER_ATTR_TENANT_TOKEN)));
    return builder.build();
  }
}
