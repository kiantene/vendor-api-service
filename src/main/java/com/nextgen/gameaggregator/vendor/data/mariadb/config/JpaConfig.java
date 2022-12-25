package com.nextgen.gameaggregator.vendor.data.mariadb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class JpaConfig {

    @Bean("dataSourceMariaReader")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.maria-reader")
    public DataSource dataSourceMariaReader() {
        return DataSourceBuilder.create().build();
    }


    @Bean("dataSourceMariaWriter")
    @ConfigurationProperties(prefix = "spring.datasource.maria-writer")
    public DataSource dataSourceMariaWriter() {
        return DataSourceBuilder.create().build();
    }

    @Bean("dataSourceMariaDefault")
    @ConfigurationProperties(prefix = "spring.datasource.maria-default")
    public DataSource dataSourceMariaDefault() {
        return DataSourceBuilder.create().build();
    }

}
