package com.learning.common.cache;

import lombok.extern.log4j.Log4j2;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import static org.redisson.config.ReadMode.MASTER_SLAVE;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Log4j2
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(value = "cache.enable", havingValue = "true")
class RedisConfiguration {
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @Bean
  RedissonClient redissonClient(
      @Value("${spring.application.name}") String clientName,
      @Value("${cache.host:redis-master}") String host,
      @Value("${cache.timeout:1000}") int timeout,
      @Value("${cache.connectTimeout:10000}") int connectTimeout) {
    log.info("Enable redis caching");
    Config config = new Config();
    config
        .useMasterSlaveServers()
        .setMasterAddress("redis://" + host + ":6379")
        .setKeepAlive(true)
        .setTimeout(timeout)
        .setConnectTimeout(connectTimeout)
        .setPingConnectionInterval(250)
        .setMasterConnectionMinimumIdleSize(10)
        .setSlaveConnectionMinimumIdleSize(10)
        .setReadMode(MASTER_SLAVE)
        .setRetryAttempts(150)
        .setRetryInterval(100)
        .setClientName(clientName);

    return Redisson.create(config);
  }
}
