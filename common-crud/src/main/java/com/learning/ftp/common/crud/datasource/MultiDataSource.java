package com.learning.ftp.common.crud.datasource;

import com.learning.ftp.common.crud.configuration.property.ConnectionProperties.DataSourceProp;
import com.learning.ftp.common.util.SecurityUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.util.StringUtils;

public class MultiDataSource implements DataSource {
  private String dbUrl;
  private List<String> schemas;
  private Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
  private String username;
  private String password;

  public MultiDataSource(List<DataSourceProp> dataSourceProps) {
    dataSources =
        dataSourceProps.stream()
            .collect(
                Collectors.toMap(
                    DataSourceProp::getCompany, ds -> new HikariDataSource(toHikariConfig(ds))));
  }

  @Override
  public Connection getConnection() throws SQLException {
    DataSource ds = getDataSource();
    return ds.getConnection();
  }

  private DataSource getDataSource() {
    if (SecurityUtil.getCurrentUser() == null) {
      return dataSources.get("default");
    }
    String tenant = SecurityUtil.getCurrentUser().getCompanyShortName();
    return StringUtils.isEmpty(tenant) || !dataSources.containsKey(tenant)
        ? dataSources.get("default")
        : dataSources.get(tenant);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    DataSource ds = getDataSource();
    return ds.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    DataSource ds = getDataSource();
    ds.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    DataSource ds = getDataSource();
    ds.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    DataSource ds = getDataSource();
    return ds.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    DataSource ds = getDataSource();
    return ds.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    DataSource ds = getDataSource();
    return ds.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    DataSource ds = getDataSource();
    return ds.isWrapperFor(iface);
  }

  private HikariConfig toHikariConfig(DataSourceProp dsProps) {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(dsProps.getJdbcUrl());
    cfg.setMinimumIdle(1);
    cfg.setMaximumPoolSize(dsProps.getMaximumPoolSize());
    cfg.setPassword(dsProps.getPassword());
    cfg.setUsername(dsProps.getUsername());
    cfg.setAutoCommit(false);
    return cfg;
  }
}
