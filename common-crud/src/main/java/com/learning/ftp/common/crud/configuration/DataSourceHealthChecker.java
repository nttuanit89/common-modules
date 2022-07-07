package com.learning.ftp.common.crud.configuration;

import com.learning.ftp.common.crud.RepoService;
import com.learning.ftp.common.restapi.HealthChecker;
import lombok.RequiredArgsConstructor;
import org.hibernate.type.StringType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@ConditionalOnBean(RepoService.class)
class DataSourceHealthChecker implements HealthChecker {

  private final RepoService repo;

  @SuppressWarnings("unchecked")
  @Override
  public boolean isGood() {
    try {
      repo.executeQuery("SELECT 1 as id ", Map.of(), Map.of("id", StringType.INSTANCE));
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
