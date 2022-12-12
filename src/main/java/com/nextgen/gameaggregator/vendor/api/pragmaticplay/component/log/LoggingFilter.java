//package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.log;
//
//import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessActionApiLogRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import org.springframework.web.util.ContentCachingRequestWrapper;
//import org.springframework.web.util.ContentCachingResponseWrapper;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
//@Component
//public class LoggingFilter extends OncePerRequestFilter {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);
//
//    @Autowired
//    private SeamlessActionApiLogRepository seamlessActionApiLogRepository;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//    }
//}
