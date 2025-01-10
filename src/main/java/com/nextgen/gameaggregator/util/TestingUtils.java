package com.nextgen.gameaggregator.util;

public class TestingUtils {

    public static String addEnvToUsername(String vendorPlayerUsername, String testEnv) {
        String envPrefix = "";
        switch (testEnv) {
            case "stg":
                envPrefix = "s";
                break;
            case "preprod":
                envPrefix = "p";
                break;
            case "dev":
                envPrefix = "q";
                break;
            case "qa":
                envPrefix = "q";
                break;
        }

        return envPrefix + vendorPlayerUsername;
    }
}
