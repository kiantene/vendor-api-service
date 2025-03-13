package com.nextgen.gameaggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TestSupportService {

    @Value("${testing.stub:false}")
    private Boolean useStub;

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

    public Boolean shouldSkipOperatorCall(String username) {
        if (isTestEnvironment()) {
            return username.toLowerCase().startsWith(usernamePrefix.toLowerCase()) || Boolean.TRUE.equals(useStub);
        }
        return false;
    }

    public Boolean isTestEnvironment() {
        return Boolean.TRUE.equals(isTestEnvironment);
    }
}
