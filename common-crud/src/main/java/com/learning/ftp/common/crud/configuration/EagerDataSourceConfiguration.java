package com.learning.ftp.common.crud.configuration;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
class EagerDataSourceConfiguration {
  @Bean
  ApplicationRunner runner(DataSource dataSource) {
    return args -> dataSource.getConnection();
  }
}
