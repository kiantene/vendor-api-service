package com.nextgen.gameaggregator.data.clickhouse.config;

import com.nextgen.gameaggregator.service.RequestService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class ClickHouseDataSourceConfig {
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    RequestService requestService;
    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseDataSourceConfig.class);

    @Value("${spring.datasource.clickhouse-default.jdbc-url}")
    private  String jdbcUrl;

    @Value("${spring.datasource.clickhouse-default.username}")
    private  String username;

    @Value("${spring.datasource.clickhouse-default.password}")
    private  String  password;

    @Value("${spring.datasource.clickhouse-default.maximum-pool-size}")
    private Integer maximumPoolSize;

    @Value("${spring.datasource.clickhouse-default.minimum-idle}")
    private Integer minimumIdle;

    @Value("${spring.datasource.clickhouse-default.connection-timeout}")
    private Integer connectionTimeout;

    @Value("${spring.datasource.clickhouse-default.idle-timeout:6000}")
    private Integer idleTimeout;

    @Bean
    public HikariDataSource clickHouseDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("ru.yandex.clickhouse.ClickHouseDriver");
        // HikariCP settings
        config.setConnectionTimeout(connectionTimeout); // milliseconds
        config.setMaximumPoolSize(maximumPoolSize); // Set the maximum number of connections in the pool
        config.setMinimumIdle(minimumIdle);  // Set the minimum number of idle connections HikariCP maintains in the pool
        config.setIdleTimeout(idleTimeout);  // Set the maximum amount of time (in milliseconds) that a connection is allowed to sit idle in the pool
        config.setPoolName("ClickHouseHikariCP");

        if (!requestService.isTestEnvironment(profilesActive)) {
            // Set SSL properties
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "strict");  // Adjust this as necessary
        }

        return new HikariDataSource(config);
    }


    @Bean
    public NamedParameterJdbcTemplate clickHouseJdbcTemplate(@Qualifier("clickHouseDataSource") HikariDataSource clickHouseDataSource) {
        return new NamedParameterJdbcTemplate(clickHouseDataSource);
    }


}
