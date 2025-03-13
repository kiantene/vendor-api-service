package com.nextgen.gameaggregator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestSupportService {

    @Value("${testing.stub-prefix:load}")
    private String usernamePrefix;

    @Value("${is-test-env:false}")
    private Boolean isTestEnvironment;

    @Value("${spring.profiles.active}")
    private String springEnv;

    public Boolean shouldSkipVendorCall(String username) {
        if (isTestEnvironment()) {
            return username.toLowerCase().startsWith(usernamePrefix.toLowerCase()) || springEnv.contains("preprod");
        }
        return false;
    }

    //For later refactor
    public Boolean shouldSkipOperatorCall(String username) {
        if (isTestEnvironment()) {
            return username.toLowerCase().startsWith(usernamePrefix.toLowerCase());
        }
        return false;
    }

    public Boolean isTestEnvironment() {
        return Boolean.TRUE.equals(isTestEnvironment);
    }
}
