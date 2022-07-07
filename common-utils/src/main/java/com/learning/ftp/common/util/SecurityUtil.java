package com.learning.ftp.common.util;

import com.learning.ftp.common.security.BaseUser;
import com.learning.proto.common.Roles;
import java.util.List;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.UUID;
import static java.util.function.Predicate.not;
import java.util.stream.Stream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class SecurityUtil {

  public static Optional<String> tenantToken() {
    return ofNullable(getCurrentUser()).map(BaseUser::getTenantToken).filter(StringUtils::hasText);
  }

  public static Optional<String> tenantContext() {
    String hostContext =
        getRequest()
            .map(ServletRequest::getServerName)
            .map(SecurityUtil::getCompanyShortName)
            .orElse(null);
    String userContext = ofNullable(getCurrentUser()).map(BaseUser::getTenantContext).orElse(null);
    String headerContext = tenantContextInHeader().orElse(null);
    return Stream.of(userContext, hostContext, headerContext)
        .filter(StringUtils::hasText)
        .findFirst();
  }

  private static String getCompanyShortName(String serverName) {
    String[] parts = serverName.split("\\.");
    String companyId = "";
    if (parts.length > 1 && !parts[0].equals("api") && !parts[0].equals("127")) {
      companyId = parts[0];
    }
    // care using wrong approach and should not be considered as a tenant
    return Optional.of(companyId)
        .map(String::toLowerCase)
        .filter(not(Set.of("care", "integration")::contains))
        .orElse(EMPTY);
  }

  private static Optional<String> tenantContextInHeader() {
    return getRequest().map(r -> r.getHeader("tenant"));
  }

  private static Optional<HttpServletRequest> getRequest() {
    return ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest);
  }

  public static String getToken() {
    BaseUser user = getCurrentUser();
    return user != null ? user.getToken() : "";
  }

  public static BaseUser getCurrentUser() {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof AnonymousAuthenticationToken) {
      return null;
    }
    OAuth2AuthenticationToken oAuthToken =
        (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    if (oAuthToken != null && (oAuthToken.getPrincipal() instanceof BaseUser)) {
      return (BaseUser) oAuthToken.getPrincipal();
    }
    return null;
  }

  public static UUID getCurrentUserId() {
    return getCurrentUser() == null
        ? null
        : UUIDUtils.fromStringSafe(getCurrentUser().getProfileId());
  }

  public static Optional<UUID> getFbProfileId() {
    return Optional.ofNullable(getCurrentUser())
        .map(user -> UUIDUtils.fromStringSafe(user.getFbProfileId()));
  }

  public static List<String> getCurrentUserRoles() {
    return getCurrentUser().getRoles();
  }

  public static boolean isCurrentUserHasRole(String roleName) {
    return getCurrentUser().hasRole(roleName);
  }

  public static boolean isCurrentUserHasRole(Roles role) {
    return getCurrentUser().hasRole(role);
  }

  public static boolean isCurrentUserHasAnyRole(Roles... roles) {
    return Stream.of(roles).anyMatch(SecurityUtil::isCurrentUserHasRole);
  }

  public static boolean isCurrentUserHasAnyRole(String... roleNames) {
    return Stream.of(roleNames).anyMatch(SecurityUtil::isCurrentUserHasRole);
  }
}
