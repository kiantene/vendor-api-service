package com.nextgen.gameaggregator.config;

import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
@EnableCouchbaseRepositories
@EnableRetry
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    @Value("${spring.couchbase.connectionString}")
    private String connectionString;
    @Value("${spring.couchbase.userName}")
    private String userName;
    @Value("${spring.couchbase.password}")
    private String password;
    @Value("${spring.couchbase.bucketName}")
    private String bucketName;
    @Value("${spring.couchbase.scopeName}")
    private String scopeName;
    @Value("${spring.couchbase.defaultTimeout:5}")
    private Long defaultSeconds;

    private String couchbaseCertName = "game_aggregator-root-certificate.pem";

    @Override
    public String getConnectionString() {
        return this.connectionString;
    }

    @Override
    protected String getScopeName() {
        return this.scopeName; // or a variable etc.;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getBucketName() {
        return this.bucketName;
    }

    @Bean
    @Primary
    public CustomConversions customConversions2() {
        return super.customConversions();
    }

    @Override
    protected void configureEnvironment(final ClusterEnvironment.Builder builder) {
        File file = null;

        try {
            File filed = new File("");
            System.out.println(filed.getAbsolutePath());
            file = ResourceUtils.getFile(filed.getAbsolutePath() + "/" + this.couchbaseCertName);

            // file = ResourceUtils.getFile("game_aggregator-root-certificate.pem");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path path = file.toPath();

        // Add the existing security configuration
        builder.securityConfig()
                .enableTls(true)
                .trustCertificate(path);

        // Add the connectTimeout configuration
        Duration defaultTimeout = Duration.ofSeconds(defaultSeconds);
        builder.timeoutConfig(TimeoutConfig
                //N1QL select timeout
                .queryTimeout(defaultTimeout)
        );
    }
}
