package com.learning.ftp.common.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import com.learning.proto.common.Roles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.slf4j.MDC;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.util.StringUtils;

public class BaseUser extends DefaultOAuth2User {
  public static final String USER_ATTR_LANGUAGE = "language";
  private static final String USER_ATTR_RESOURCE_ID = "resourceId";
  public static final String USER_ATTR_REQUEST_ID = "requestId";
  public static final String USER_ATTR_HOST = "host";
  private static final String USER_ATTR_USER_ID = "userId";
  public static final String USER_ATTR_TENANT_CONTEXT = "tenant";
  public static final String USER_ATTR_TENANT_TOKEN = "x-tenant-token";
  private final String token;
  private Date expireTime;
  private Map<String, Object> userAttributes;

  public static final String USER_ATTR_PROFILE_ID = "profileId";
  public static final String USER_ATTR_TENANT_PROFILE_ID = "tenantProfileId";
  public static final String USER_ATTR_COMPANY_SHORT_NAME = "companyShortName";
  public static final String USER_ATTR_IS_ADMIN = "isAdmin";

  public static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
  public static final String CLAIM_ROLE = "role";
  public static final String CLAIM_SESSION_STATE = "session_state";

  @SafeVarargs
  public BaseUser(
      String token,
      Collection<GrantedAuthority> authorities,
      Map<String, Object> attributes,
      String nameAttributeKey,
      Map<String, String>... params) {
    super(authorities, attributes, nameAttributeKey);
    this.token = token;
    extractUserInfo(attributes, params);
  }

  public BaseUser(
      String realm,
      String token,
      Date expireTime,
      Set<GrantedAuthority> authorities,
      Map<String, Object> attributes,
      String nameAttributeKey) {
    super(authorities, attributes, nameAttributeKey);
    this.token = token;
    this.expireTime = expireTime;
  }

  public BaseUser(
      String realm,
      String token,
      Date expireTime,
      Set<GrantedAuthority> authorities,
      Map<String, Object> attributes,
      String nameAttributeKey,
      Map<String, Object> userAttributes) {
    super(authorities, attributes, nameAttributeKey);
    this.token = token;
    this.expireTime = expireTime;
    this.userAttributes = userAttributes;
  }

  public String getProfileId() {
    return ofNullable(emptyToNull((String) this.userAttributes.get(USER_ATTR_TENANT_PROFILE_ID)))
        .orElse((String) this.userAttributes.get(USER_ATTR_PROFILE_ID));
  }

  public String getFbProfileId() {
    return (String) this.userAttributes.get(USER_ATTR_PROFILE_ID);
  }

  public String getCompanyShortName() {
    return (String) this.userAttributes.get(USER_ATTR_COMPANY_SHORT_NAME);
  }

  public String getTenantContext() {
    return (String) this.userAttributes.get(USER_ATTR_TENANT_CONTEXT);
  }

  public String getTenantToken() {
    return (String) this.userAttributes.get(USER_ATTR_TENANT_TOKEN);
  }

  public String getRequestId() {
    return (String) this.userAttributes.get(USER_ATTR_REQUEST_ID);
  }

  public String getHost() {
    return (String) this.userAttributes.get(USER_ATTR_HOST);
  }

  public boolean isAdmin() {
    return (Boolean) this.userAttributes.get(USER_ATTR_IS_ADMIN);
  }

  public String getUsername() {
    return (String) this.userAttributes.get(CLAIM_PREFERRED_USERNAME);
  }

  public String getResourceId() {
    if (getAttributes().containsKey(USER_ATTR_RESOURCE_ID)) {
      return (String) getAttributes().get(USER_ATTR_RESOURCE_ID);
    }
    return null;
  }

  public String getToken() {
    return token;
  }

  public Date getExpireTime() {
    return expireTime;
  }

  public String getLanguage() {
    return this.userAttributes.get(USER_ATTR_LANGUAGE).toString();
  }

  public List<String> getRoles() {
    return Arrays.asList(getRole().split("\\s*,\\s*"));
  }

  public String getRole() {
    return this.userAttributes.get(CLAIM_ROLE).toString();
  }

  public String getSessionId() {
    return (String) this.getUserAttributes().get(CLAIM_SESSION_STATE);
  }

  public boolean hasRole(String roleName) {
    return getRoles().contains(String.format("%s_%s", getCompanyShortName(), roleName));
  }

  public boolean hasRole(Roles role) {
    return hasRole(role.name());
  }

  public boolean hasAnyRole(Roles... roles) {
    return Stream.of(roles).anyMatch(this::hasRole);
  }

  public boolean hasAnyRole(String... roleNames) {
    return Stream.of(roleNames).anyMatch(this::hasRole);
  }

  public Map<String, Object> getUserAttributes() {
    return Collections.unmodifiableMap(this.userAttributes);
  }

  private void extractUserInfo(Map<String, Object> attributes, Map<String, String>... info) {
    DecodedJWT decodedJwt = JWT.decode(this.token);
    Map<String, String> infoAttrs = info.length > 0 ? info[0] : Collections.emptyMap();
    String tenantProfileId =
        getOrDefaultFromToken(decodedJwt, infoAttrs, USER_ATTR_TENANT_PROFILE_ID);
    String profileId = getOrDefaultFromToken(decodedJwt, infoAttrs, USER_ATTR_PROFILE_ID);
    String tenantToken = infoAttrs.get(USER_ATTR_TENANT_TOKEN);
    String tenantContext = infoAttrs.get(USER_ATTR_TENANT_CONTEXT);
    String companyShortName =
        ofNullable(tenantContext).filter(StringUtils::hasText).orElse(getTenant(decodedJwt, info));
    Boolean isAdmin =
        Boolean.parseBoolean(getOrDefaultFromToken(decodedJwt, infoAttrs, USER_ATTR_IS_ADMIN));
    String username = getOrDefaultFromToken(decodedJwt, infoAttrs, CLAIM_PREFERRED_USERNAME);
    String role = getOrDefaultFromToken(decodedJwt, infoAttrs, CLAIM_ROLE);
    String requestId =
        ofNullable(attributes.get(USER_ATTR_REQUEST_ID))
            .map(String::valueOf)
            .filter(StringUtils::hasText)
            .orElse("non-restfull-request" + UUID.randomUUID());
    String host =
        attributes.containsKey(USER_ATTR_HOST)
            ? attributes.get(USER_ATTR_HOST).toString()
            : "localhost";
    String session = decodedJwt.getClaim(CLAIM_SESSION_STATE).asString();

    Map<String, Object> userAttr = new HashMap<>();
    userAttr.put(USER_ATTR_PROFILE_ID, nullToEmpty(profileId));
    userAttr.put(USER_ATTR_TENANT_PROFILE_ID, nullToEmpty(tenantProfileId));
    userAttr.put(USER_ATTR_COMPANY_SHORT_NAME, nullToEmpty(companyShortName));
    userAttr.put(USER_ATTR_TENANT_CONTEXT, nullToEmpty(tenantContext));
    userAttr.put(USER_ATTR_TENANT_TOKEN, nullToEmpty(tenantToken));
    userAttr.put(USER_ATTR_IS_ADMIN, isAdmin != null && isAdmin);
    userAttr.put(CLAIM_PREFERRED_USERNAME, nullToEmpty(username));
    userAttr.put(CLAIM_ROLE, nullToEmpty(role));
    userAttr.put(USER_ATTR_REQUEST_ID, nullToEmpty(requestId));
    userAttr.put(USER_ATTR_HOST, nullToEmpty(host));
    userAttr.put(CLAIM_SESSION_STATE, nullToEmpty(session));
    this.userAttributes = userAttr;

    MDC.put(USER_ATTR_REQUEST_ID, requestId);
    MDC.put(USER_ATTR_HOST, host);
    MDC.put(USER_ATTR_USER_ID, profileId);
  }

  private String getOrDefaultFromToken(
      DecodedJWT decodedJwt, Map<String, String> infoAttrs, String key) {
    String value = decodedJwt.getClaim(key).asString();
    return ofNullable(infoAttrs.get(key)).filter(StringUtils::hasText).orElse(value);
  }

  private String getTenant(DecodedJWT decodedJwt, Map<String, String>... info) {
    Map<String, String> infoAttrs = info.length > 0 ? info[0] : Collections.emptyMap();
    String companyShortName = decodedJwt.getClaim(USER_ATTR_COMPANY_SHORT_NAME).asString();
    if (isEmpty(companyShortName) && infoAttrs.containsKey(USER_ATTR_COMPANY_SHORT_NAME)) {
      companyShortName = infoAttrs.get(USER_ATTR_COMPANY_SHORT_NAME);
    }
    return companyShortName;
  }
}
