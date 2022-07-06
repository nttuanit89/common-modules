package com.learning.common.grpc;

import com.learning.common.logging.BaseLoggerFactory;
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
import static com.learning.common.util.SecurityUtil.getCurrentUser;
import static com.learning.common.util.SecurityUtil.getToken;
import static com.learning.common.util.SecurityUtil.tenantContext;

import com.google.common.collect.Maps;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import java.lang.reflect.Method;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import org.springframework.stereotype.Service;
import static org.springframework.util.ReflectionUtils.invokeMethod;

@SuppressWarnings("unchecked")
@Service
@Log4j2
public class GRPCStubProvider {
  private static final Logger LOG = BaseLoggerFactory.getLogger(GRPCStubProvider.class);
  private final Map<String, ManagedChannel> channels = Maps.newConcurrentMap();
  private final ThreadLocal<Map<Class<?>, AbstractStub<?>>> stubs =
      ThreadLocal.withInitial(Maps::newConcurrentMap);
  private final ApplicationContext applicationContext;
  private final int defaultMaxMessageSize;

  GRPCStubProvider(
      ApplicationContext applicationContext,
      @Value("${grpc.messageSize:16777216}") int defaultMaxMessageSize) {
    this.applicationContext = applicationContext;
    this.defaultMaxMessageSize = defaultMaxMessageSize;
  }

  public <T extends AbstractStub<T>> T getBlockingStub(String service, Class<T> clazz) {
    return getStub(clazz, service);
  }

  private <T extends AbstractStub<T>> T getStub(Class<T> clazz, String service) {
    try {
      Metadata header = new Metadata();
      header.put(key(AUTHORIZATION), String.format("Bearer %s", getToken()));
      putMeta(header, USER_ATTR_REQUEST_ID, BaseUser::getRequestId);
      putMeta(header, USER_ATTR_HOST, BaseUser::getHost);
      putMeta(header, USER_ATTR_COMPANY_SHORT_NAME, BaseUser::getCompanyShortName);
      putMeta(header, USER_ATTR_TENANT_CONTEXT, u -> tenantContext().orElse(EMPTY));
      putMeta(header, USER_ATTR_PROFILE_ID, BaseUser::getProfileId);
      putMeta(header, USER_ATTR_TENANT_PROFILE_ID, BaseUser::getProfileId);
      putMeta(header, CLAIM_ROLE, BaseUser::getRole);
      putMeta(header, CLAIM_PREFERRED_USERNAME, BaseUser::getUsername);
      putMeta(header, USER_ATTR_IS_ADMIN, u -> Boolean.toString(u.isAdmin()));
      putMeta(header, USER_ATTR_TENANT_TOKEN, BaseUser::getTenantToken);

      ManagedChannel channel = channels.computeIfAbsent(service, this::createChannel);
      T stub = (T) stubs.get().computeIfAbsent(clazz, k -> createBlockingStub(channel, clazz));
      return MetadataUtils.attachHeaders(stub, header);
    } catch (Exception ex) {
      LOG.error("Error when get stub: " + ex.getMessage(), ex);
      channels.remove(service);
      throw new RuntimeException("Error when get stub", ex);
    }
  }

  private void putMeta(Metadata header, String key, Function<BaseUser, String> provider) {
    Optional<BaseUser> currentUser = ofNullable(getCurrentUser());
    currentUser.map(provider).filter(Strings::isNotBlank).ifPresent(v -> header.put(key(key), v));
  }

  private Metadata.Key<String> key(String key) {
    return Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
  }

  private <T extends AbstractStub<T>> T createBlockingStub(ManagedChannel channel, Class<T> clazz) {
    try {
      Class<?> enclosingClass = clazz.getEnclosingClass();
      Method stubMethod = enclosingClass.getDeclaredMethod("newBlockingStub", Channel.class);
      T stub = (T) invokeMethod(stubMethod, null, channel);
      return requireNonNull(stub)
          .withCompression("gzip")
          .withMaxOutboundMessageSize(defaultMaxMessageSize)
          .withWaitForReady();
    } catch (Exception ex) {
      String msg = "Error when create blocking stub: " + ex.getMessage();
      LOG.error(msg, ex);
      throw new RuntimeException(msg);
    }
  }

  private ManagedChannel createChannel(String service) {
    return getBuilder(service)
        .usePlaintext()
        .directExecutor()
        .enableRetry()
        .enableFullStreamDecompression()
        .keepAliveTime(30, SECONDS)
        .withOption(CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(75))
        .maxInboundMessageSize(defaultMaxMessageSize)
        .maxInboundMetadataSize(defaultMaxMessageSize)
        .defaultLoadBalancingPolicy("round_robin")
        .build();
  }

  private NettyChannelBuilder getBuilder(String service) {
    String serviceConfig = "external." + service;
    String host = getConfig(serviceConfig + ".host", service, String.class);
    int port = getConfig(serviceConfig + ".port", 7080, int.class);
    return (NettyChannelBuilder) ManagedChannelBuilder.forAddress(host, port);
  }

  private <T> T getConfig(String config, T defaultValue, Class<T> clazz) {
    Environment env = applicationContext.getEnvironment();
    return env.containsProperty(config) ? env.getProperty(config, clazz) : defaultValue;
  }
}
