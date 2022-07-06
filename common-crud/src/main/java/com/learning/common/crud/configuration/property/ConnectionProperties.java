package com.learning.common.crud.configuration.property;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "multi-datasource")
@Configuration
@Getter
@Setter
public class ConnectionProperties {
  private List<DataSourceProp> datasources;

  @Getter
  @Setter
  public static class DataSourceProp {
    private String company;
    private String jdbcUrl;
    private String username;
    private String password;
    private int maximumPoolSize;
  }
}
