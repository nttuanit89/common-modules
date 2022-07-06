package com.learning.common.crud.configuration;

import com.learning.common.crud.configuration.property.ConnectionProperties;
import com.learning.common.crud.datasource.MultiDataSource;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty("multi-datasource.enable")
class DataSourceConfiguration {
  private ConnectionProperties connectionProperties;

  @Autowired
  public DataSourceConfiguration(ConnectionProperties connectionProperties) {
    this.connectionProperties = connectionProperties;
  }

  @Bean(name = "multiDataSource")
  public MultiDataSource multiDataSource() {
    return new MultiDataSource(connectionProperties.getDatasources());
  }

  @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      @Qualifier("multiDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
        new LocalContainerEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource);
    entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    entityManagerFactoryBean.setPackagesToScan("com.learning");

    Properties jpaProperties = new Properties();
    jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
    jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", false);
    jpaProperties.put("hibernate.implicit_naming_strategy", new SpringImplicitNamingStrategy());
    jpaProperties.put("hibernate.physical_naming_strategy", new SpringPhysicalNamingStrategy());
    entityManagerFactoryBean.setJpaProperties(jpaProperties);
    entityManagerFactoryBean.afterPropertiesSet();
    return entityManagerFactoryBean;
  }

  @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(emf);
    return transactionManager;
  }
}
