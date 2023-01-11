package com.nextgen.gameaggregator.data.mariadb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class JpaConfig {

    @Bean("dataSourceMariaDefault")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.maria-default")
    public DataSource dataSourceMariaDefault() {
        return DataSourceBuilder.create().build();
    }

}
