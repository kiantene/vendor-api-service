package com.nextgen.gameaggregator.data.mariadb.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class JpaConfig {

    @Bean("GaServiceWriterDb")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.maria-default")
    public DataSource GaServiceWriterDb() {
        return DataSourceBuilder.create().build();
    }

    @Bean("GaServiceReaderDb")
    @ConfigurationProperties(prefix = "spring.datasource.game-aggregator-service-reader")
    public DataSource GaServiceReaderDb() {
        return DataSourceBuilder.create().build();
    }

    @Bean("walletServiceWriterDb")
    @ConfigurationProperties(prefix = "spring.datasource.wallet-service-writer")
    public DataSource walletServiceWriterDb() {
        return DataSourceBuilder.create().build();
    }

    @Bean("walletServiceReaderDb")
    @ConfigurationProperties(prefix = "spring.datasource.wallet-service-reader")
    public DataSource walletServiceReaderDb() {
        return DataSourceBuilder.create().build();
    }


    @Bean(name = "chainedTransactionManager")
    public ChainedTransactionManager transactionManager(@Qualifier("transactionManagerGaServiceWriterDb") PlatformTransactionManager GaServiceWriterDb,
                                                        @Qualifier("transactionManagerGaServiceReaderDb") PlatformTransactionManager GaServiceReaderDb,
                                                        @Qualifier("transactionManagerWalletServiceWriterDb") PlatformTransactionManager walletServiceWriterDb,
                                                        @Qualifier("transactionManagerWalletServiceReaderDb") PlatformTransactionManager walletServiceReaderDb
    ) {
        return new ChainedTransactionManager(GaServiceWriterDb,  GaServiceReaderDb, walletServiceWriterDb, walletServiceReaderDb

        );
    }
}
