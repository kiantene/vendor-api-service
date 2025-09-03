package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.util.EnvUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Service
public class TestSupportService {

    private final String skipVendorUserPrefix;
    private final String skipOperatorUserPrefix;
    private final Boolean isTestEnvironment;
    private final String springEnv;
    private final String envPrefixVendorList;

    public TestSupportService(
            @Value("${testing.skip-vendor-prefix:load}") String skipVendorUserPrefix,
            @Value("${testing.skip-operator-prefix:load}") String skipOperatorUserPrefix,
            @Value("${is-test-env:false}") Boolean isTestEnvironment,
            @Value("${spring.profiles.active}") String springEnv,
            @Value("${testing.prefix-vendor-list:}") String envPrefixVendorList
    ) {
        this.skipVendorUserPrefix = skipVendorUserPrefix;
        this.skipOperatorUserPrefix = skipOperatorUserPrefix;
        this.isTestEnvironment = isTestEnvironment;
        this.springEnv = springEnv;
        this.envPrefixVendorList = envPrefixVendorList;
    }

    public Boolean shouldSkipVendorCall(String username) {
        Boolean skipCall = false;
        if (isTestEnvironment()) {
            if (springEnv.contains("preprod")) {
                return true;
            }

            List<String> userPrefixList = getPrefixListFromString(skipVendorUserPrefix);

            for (String userPrefix : userPrefixList) {
                if (userPrefix.isBlank()) {
                    continue;
                }
                if (username.toLowerCase().startsWith(userPrefix.toLowerCase())) {
                    skipCall = true;
                    break;
                }
            }
        }
        return skipCall;
    }

    public Boolean shouldSkipOperatorCall(String username) {
        Boolean skipCall = false;
        if (isTestEnvironment()) {

            List<String> userPrefixList = getPrefixListFromString(skipOperatorUserPrefix);

            for (String userPrefix : userPrefixList) {
                if (userPrefix.isBlank()) {
                    continue;
                }
                if (username.toLowerCase().startsWith(userPrefix.toLowerCase())) {
                    skipCall = true;
                    break;
                }
            }
        }

        return skipCall;
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

    public List<String> getPrefixListFromString(String prefixListString) {
        List<String> prefixList = Arrays.asList(prefixListString.split(","));

        return prefixList;
    }

    public Boolean isTestEnvironment() {
        return Boolean.TRUE.equals(isTestEnvironment);
    }
}
