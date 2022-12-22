package com.nextgen.gameaggregator.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;


@Configuration
@ServletComponentScan
@SpringBootApplication
@EntityScan("com.nextgen.gameaggregator.vendor.*")
@ComponentScan(basePackages = {
		"com.nextgen.sas",
		"com.nextgen.gameaggregator.vendor",
		"com.nextgen.sas.core"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class GameAggregatorVendorApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameAggregatorVendorApiServiceApplication.class, args);
	}

}
