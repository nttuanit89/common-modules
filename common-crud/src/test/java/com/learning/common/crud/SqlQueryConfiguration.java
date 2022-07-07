package com.learning.common.crud;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

@Configuration
class SqlQueryConfiguration {
  @Bean
  String findEmployeesByPhones(@Value("classpath:/query/findEmployeesByPhones.sql") Resource query)
      throws IOException {
    return getString(query);
  }

  @Bean
  String findEmployeesIdByPhones(
      @Value("classpath:/query/findEmployeesIdByPhones.sql") Resource query) throws IOException {
    return getString(query);
  }

  private String getString(Resource query) throws IOException {
    try (Reader reader = new InputStreamReader(query.getInputStream(), UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    }
  }
}
