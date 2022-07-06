package com.learning.common.grpc;

import static com.google.common.base.Strings.nullToEmpty;
import com.google.common.collect.ImmutableMap;
import com.learning.common.security.BaseUser;
import static com.learning.common.security.BaseUser.CLAIM_PREFERRED_USERNAME;
import static com.learning.common.security.BaseUser.CLAIM_ROLE;
import static com.learning.common.security.BaseUser.USER_ATTR_COMPANY_SHORT_NAME;
import static com.learning.common.security.BaseUser.USER_ATTR_HOST;
import static com.learning.common.security.BaseUser.USER_ATTR_IS_ADMIN;
import static com.learning.common.security.BaseUser.USER_ATTR_PROFILE_ID;
import static com.learning.common.security.BaseUser.USER_ATTR_REQUEST_ID;
import static com.learning.common.security.BaseUser.USER_ATTR_TENANT_CONTEXT;
import static com.learning.common.security.BaseUser.USER_ATTR_TENANT_PROFILE_ID;
import static com.learning.common.security.BaseUser.USER_ATTR_TENANT_TOKEN;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import java.util.Map;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

@GRpcGlobalInterceptor
public class RequestInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    String token = nullToEmpty(headers.get(key(AUTHORIZATION)));
    ServerCall.Listener<ReqT> listener = null;
    if (token.startsWith("Bearer ")) {
      token = token.substring(7);
    }

    String requestId = nullToEmpty(headers.get(key(USER_ATTR_REQUEST_ID)));
    String host = nullToEmpty(headers.get(key(USER_ATTR_HOST)));
    Map<String, Object> attributes =
        ImmutableMap.of(USER_ATTR_REQUEST_ID, requestId, USER_ATTR_HOST, host);

    BaseUser user =
        new BaseUser(
            token,
            singletonList(new SimpleGrantedAuthority("ROLE_Default")),
            attributes,
            USER_ATTR_REQUEST_ID,
            getInfo(headers));
    Authentication authentication = new OAuth2AuthenticationToken(user, emptySet(), "finbase");

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
        next.startCall(call, headers)) {
      @Override
      public void onHalfClose() {
        try {
          // unary calls may error out here
          SecurityContextHolder.getContext().setAuthentication(authentication);
          super.onHalfClose();
        } catch (Exception aex) {
          if (aex instanceof AccessDeniedException) {
            call.close(
                Status.PERMISSION_DENIED.withCause(aex).withDescription(aex.getMessage()),
                new Metadata());
          } else {
            call.close(
                Status.UNKNOWN.withCause(aex).withDescription(aex.getMessage()), new Metadata());
          }
        } finally {
          SecurityContextHolder.clearContext();
        }
      }
    };
  }

  private Metadata.Key<String> key(String key) {
    return Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
  }

  @SuppressWarnings("ConstantConditions")
  private Map<String, String> getInfo(Metadata headers) {
    return Stream.of(
            USER_ATTR_TENANT_CONTEXT,
            USER_ATTR_TENANT_TOKEN,
            USER_ATTR_COMPANY_SHORT_NAME,
            USER_ATTR_PROFILE_ID,
            USER_ATTR_TENANT_PROFILE_ID,
            CLAIM_ROLE,
            CLAIM_PREFERRED_USERNAME,
            USER_ATTR_IS_ADMIN)
        .filter(k -> headers.containsKey(key(k)))
        .collect(Collectors.toMap(identity(), k -> headers.get(key(k))));
  }
}
