package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
        entityManagerFactoryRef = "entityManagerFactoryMariaWriter",//配置连接工厂 entityManagerFactory
        transactionManagerRef = "transactionManagerMariaWriter", //配置 事物管理器  transactionManager
        basePackages = {"com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.manager"}//设置持久层所在位置
)
public class MariaWriterDataSourceConfig {

    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private HibernateProperties hibernateProperties;
    // 自动注入配置好的数据源
    @Autowired
    @Qualifier("dataSourceMariaWriter")
    private DataSource mariaDataSource;
    // 获取对应的数据库方言
    @Value("${spring.jpa.properties.hibernate.maria-dialect}")
    private String mariaDialect;

    @Primary
    @Bean(name = "entityManagerFactoryMariaWriter")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryMariaWriter(EntityManagerFactoryBuilder builder) {
        Map<String, String> map = new HashMap<>();
        // 设置对应的数据库方言
        map.put("hibernate.dialect", mariaDialect);
        jpaProperties.setProperties(map);
        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
        return builder
                //设置数据源
                .dataSource(mariaDataSource)
                //设置数据源属性
                .properties(properties)
                //设置实体类所在位置.扫描所有带有 @Entity 注解的类
                .packages("com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.entity")
                // Spring会将EntityManagerFactory注入到Repository之中.有了 EntityManagerFactory之后,
                // Repository就能用它来创建 EntityManager 了,然后 EntityManager 就可以针对数据库执行操作
                .persistenceUnit("mysqlPersistenceUnit")
                .build();
    }

    @Bean(name = "transactionManagerMariaWriter")
    PlatformTransactionManager transactionManagerMariaWriter(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactoryMariaWriter(builder).getObject());
    }

}
