package com.learning.ftp.common.cache;

import com.google.common.collect.Maps;
import java.util.Map;
import static java.util.Objects.nonNull;
import java.util.Optional;
import static java.util.Optional.empty;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.LocalCachedMapOptions;
import static org.redisson.api.LocalCachedMapOptions.EvictionPolicy.LFU;
import static org.redisson.api.LocalCachedMapOptions.ReconnectionStrategy.LOAD;
import static org.redisson.api.LocalCachedMapOptions.SyncStrategy.INVALIDATE;
import org.redisson.api.RMap;
import org.redisson.api.RSetCache;

@Log4j2
public abstract class AbstractCache {
  @SuppressWarnings("rawtypes")
  protected final Map<String, RMap> maps = Maps.newConcurrentMap();

  @SuppressWarnings("rawtypes")
  protected final Map<String, RSetCache> sets = Maps.newConcurrentMap();

  protected <K, V> LocalCachedMapOptions<K, V> defaults() {
    return LocalCachedMapOptions.<K, V>defaults()
        .cacheSize(1000)
        .evictionPolicy(LFU)
        .reconnectionStrategy(LOAD)
        .syncStrategy(INVALIDATE);
  }

  protected <T> T compute(String key, Supplier<T> supplier, RMap<String, T> cache) {
    T old = cache.get(key);
    if (nonNull(old)) {
      return old;
    }

    var value = supplier.get();
    if (nonNull(value)) {
      cache.putAsync(key, value);
    }

    return value;
  }

  protected <T> Optional<T> safeOperate(Supplier<Optional<T>> supplier) {
    try {
      return supplier.get();
    } catch (Exception e) {
      log.warn("{} fail to get cache: {}", getClass(), e.getLocalizedMessage());
      return empty();
    }
  }

  protected void safeOperate(Runnable runnable) {
    safeOperate(
        () -> {
          runnable.run();
          return empty();
        });
  }
}
