package com.nextgen.gameaggregator.vendor.whitecliff.constant;

public class Credentials {
    public static final String API_URL = "api_url";

    public static final String AG_TOKEN = "ag_token";

    public static final String AG_CODE = "ag_code";

    public static final String SECRET_KEY = "secret_key";

    public static final String PRODUCT_ID = "product_id";

    private Credentials() {
    }

    public static Credentials createCredentials() {
        return new Credentials();
    }
}
