package com.nextgen.gameaggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.couchbase.core.convert.CustomConversions;

@Configuration
@ServletComponentScan
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class })
@EntityScan("com.nextgen.gameaggregator.*")
@ComponentScan(basePackages = {
		"com.nextgen.sas.core",
		"com.nextgen.gameaggregator",
		"com.nextgen.gameaggregator.vendor",
},nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class GameAggregatorVendorApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameAggregatorVendorApiServiceApplication.class, args);
	}

}
