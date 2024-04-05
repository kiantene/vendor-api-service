package com.nextgen.gameaggregator.custodianseamless.service;

import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RequestTrackerService {

    private final Map<String, Long> requestMap = new ConcurrentHashMap<>();

    public void isNewRequest(String requestBody, Long timeWindow)  throws DuplicateRequestException {


        synchronized (requestMap) {

            String uniqueRequestBody = DigestUtils.md5Hex(requestBody);
            Long lastRequestTime = requestMap.get(uniqueRequestBody);
            long currentTime = System.currentTimeMillis();

            if (lastRequestTime == null || currentTime - lastRequestTime > timeWindow) {
                requestMap.put(uniqueRequestBody, currentTime);
            } else {
                throw new DuplicateRequestException("duplicate request :"+requestBody);
            }
        }
    }
}
