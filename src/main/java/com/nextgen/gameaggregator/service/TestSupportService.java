package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.util.EnvUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class TestSupportService {

    @Value("${testing.stub-prefix:load}")
    private String usernamePrefix;

    @Value("${is-test-env:false}")
    private Boolean isTestEnvironment;

    @Value("${spring.profiles.active}")
    private String springEnv;

    @Value("${testing.prefix-vendor-list:}")
    private String envPrefixVendorList;

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

    public String appendEnvPrefixToVendorUsername(String username, Integer vendorId) {
        if (isTestEnvironment()) {
            HashSet<Integer> envPrefixVendors = EnvUtils.getVendorHashSetFromEnv(envPrefixVendorList);
            if (envPrefixVendors.contains(vendorId)) {
                username = getEnvPrefix() + username;
            }
        }
        return username;
    }

    public String getEnvPrefix() {
        return switch (springEnv) {
            case "stg" -> "s";
            case "preprod" -> "p";
            case "dev", "qa" -> "q";
            default -> "";
        };
    }

    public Boolean isTestEnvironment() {
        return Boolean.TRUE.equals(isTestEnvironment);
    }



}
