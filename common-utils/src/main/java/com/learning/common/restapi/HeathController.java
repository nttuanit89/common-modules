package com.learning.common.restapi;

import com.learning.common.util.ExceptionUtil;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcServerRunner;
import org.lognet.springboot.grpc.context.GRpcServerInitializedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@RestController
@Log4j2
@Lazy
class HeathController {
  private final List<HealthChecker> checkers;

  @PostConstruct
  public void test() {
    log.info("Initialized Health checkers");
  }

  @GetMapping("/health")
  String healthCheck() {
    if (checkers.stream().allMatch(HealthChecker::isGood)) {
      return "All good";
    }

    throw ExceptionUtil.error("0000000", SERVICE_UNAVAILABLE, "Not good!").get();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @RequiredArgsConstructor
  @Component
  @ConditionalOnClass(GRpcServerInitializedEvent.class)
  static class GrpcServerHealthChecker implements HealthChecker {
    private boolean isLive;
    private final Optional<GRpcServerRunner> runner;

    @Override
    public boolean isGood() {
      return isLive || runner.isEmpty();
    }

    @EventListener
    void handle(GRpcServerInitializedEvent event) {
      isLive = true;
    }
  }
}
