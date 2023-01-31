package com.nextgen.gameaggregator.config;

import com.nextgen.sas.core.web.interceptor.ActionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Component

public class MvcConfig implements WebMvcConfigurer {

    private static final List<String> WHITELIST_URL = Arrays.asList("/login", "/logout");

    @Autowired
    private ActionInterceptor actionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration = registry.addInterceptor(actionInterceptor);
        registration.excludePathPatterns(WHITELIST_URL);
        registration.addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                //	.allowedOrigins("*")
                .allowedOriginPatterns("https://*.gasea168.com")
                .allowedMethods("*")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }
}

