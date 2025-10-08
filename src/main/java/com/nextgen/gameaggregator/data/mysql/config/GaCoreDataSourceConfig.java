package com.nextgen.gameaggregator.data.mysql.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.nextgen.gameaggregator.core.repository",
        entityManagerFactoryRef = "entityManagerFactoryLibReadOnly",
        transactionManagerRef = "transactionManagerLibReadOnly"
)
public class GaCoreDataSourceConfig {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private HibernateProperties hibernateProperties;

    @Value("${spring.jpa.properties.hibernate.maria-dialect}")
    private String mariaDialect;

    @Autowired
    @Qualifier("GaServiceWriterDb")
    private DataSource sharedDataSource; // reuse connection pool from GaServiceWriterDataSourceConfig

    @Bean(name = "entityManagerFactoryLibReadOnly")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryLibReadOnly(
            EntityManagerFactoryBuilder builder
    ) {
        Map<String, String> map = new HashMap<>();
        map.put("hibernate.dialect", mariaDialect);
        map.put("hibernate.hbm2ddl.auto", "none");
        // (optional) align naming strategies with your main PU if you set them there
        // map.put("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        // map.put("hibernate.physical_naming_strategy",  "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");

        jpaProperties.setProperties(map);
        Map<String, Object> properties =
                hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());

        return builder
                .dataSource(sharedDataSource)
                .properties(properties)
                .packages("com.nextgen.gameaggregator.core.entity")
                .persistenceUnit("mysqlPersistenceUnitGaLibReadOnly")
                .build();
    }

    @Bean(name = "transactionManagerLibReadOnly")
    public PlatformTransactionManager transactionManagerLibReadOnly(
            @Qualifier("entityManagerFactoryLibReadOnly")
            LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
