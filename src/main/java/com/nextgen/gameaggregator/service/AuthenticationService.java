package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.util.ApiSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthenticationService {

    public String generateSignature(Object payload, String apiSecret) {

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(payload);
        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);

        return actualSignature;
    }

    public String generateSignatureWithJson(String jsonPayload, String apiSecret) {

        String actualSignature = ApiSecurityUtils.getHmacSignature(jsonPayload, apiSecret);

        return actualSignature;
    }

}
