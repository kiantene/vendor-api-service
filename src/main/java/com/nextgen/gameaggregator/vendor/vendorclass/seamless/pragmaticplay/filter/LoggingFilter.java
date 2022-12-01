package com.nextgen.gameaggregator.vendor.vendorclass.seamless.pragmaticplay.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class LoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;

        String requestBody = getStringValue(requestWrapper.getContentAsByteArray(),
                request.getCharacterEncoding());
        String responseBody = getStringValue(responseWrapper.getContentAsByteArray(),
                response.getCharacterEncoding());

//        LOGGER.info(
//                "\n=====================================================" +
//                "\nFINISHED PROCESSING : METHOD={}; REQUESTURI={}; " +
//                        "\nREQUEST PAYLOAD={}; RESPONSE CODE={}; " +
//                        "\nRESPONSE={}; TIM TAKEN={}" +
//                        "\n=====================================================",
//                request.getMethod(), request.getRequestURI(), requestBody, response.getStatus(), responseBody,
//                timeTaken);

        LOGGER.info("\n*RESPONSE : \n{} ;\n*TIME TAKEN : {} "+
                        "\n=========================================AGGREGATOR END===============================================\n",
                responseBody,timeTaken );
        responseWrapper.copyBodyToResponse();
    }

    private String getStringValue(byte[] contentAsByteArray, String characterEncoding) {
        try {
            return new String(contentAsByteArray, 0, contentAsByteArray.length, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
