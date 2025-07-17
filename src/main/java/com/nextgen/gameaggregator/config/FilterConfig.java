package com.nextgen.gameaggregator.config;


import com.nextgen.gameaggregator.core.filter.RequestLoggingFilter;
//import com.nextgen.gameaggregator.core.filter.SignatureValidationFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> loggingFilter(RequestLoggingFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(1); // Logging comes first
        return registration;
    }

//    @Bean
//    public FilterRegistrationBean<Filter> signatureFilter(SignatureValidationFilter filter) {
//        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
//        registration.setFilter(filter);
//        registration.setOrder(2); // Signature validation after logging
//        return registration;
//    }
}
