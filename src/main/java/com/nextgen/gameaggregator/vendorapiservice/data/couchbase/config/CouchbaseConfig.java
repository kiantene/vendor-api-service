package com.nextgen.gameaggregator.vendorapiservice.data.couchbase.config;
import com.couchbase.client.java.env.ClusterEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    private String connectionString = "couchbases://cb.8vuq-3cvys0flbkz.cloud.couchbase.com";
    private String userName = "dev";
    private String password = "Asdf1234@";

    @Override
    public String getConnectionString() {
        return this.connectionString;

    }

    @Override
    protected String getScopeName() {
        return "raw"; // or a variable etc.;
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
        return "game_aggregator";
    }

    @Bean
    public CustomConversions customConversions() {
        return super.customConversions();
    }

    @Override
    protected void configureEnvironment(final ClusterEnvironment.Builder builder) {

        File file = null;
        try {
            File filed= new File("");
            System.out.println(filed.getAbsolutePath());
            file = ResourceUtils.getFile(filed.getAbsolutePath()+"/game_aggregator-root-certificate.pem");

            //  file = ResourceUtils.getFile("game_aggregator-root-certificate.pem");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path path = file.toPath();
        builder.securityConfig().enableTls(true).trustCertificate( path);
    }

}
