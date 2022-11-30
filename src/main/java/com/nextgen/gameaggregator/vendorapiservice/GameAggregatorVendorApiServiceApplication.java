package com.nextgen.gameaggregator.vendorapiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;


@ServletComponentScan
@SpringBootApplication
@EntityScan("com.nextgen.gameaggregator.vendorapiservice.*")
@ComponentScan(basePackages = {
		"com.nextgen.sas",
		"com.nextgen.gameaggregator.vendorapiservice",
		"com.nextgen.sas.core"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class GameAggregatorVendorApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameAggregatorVendorApiServiceApplication.class, args);
	}

}
