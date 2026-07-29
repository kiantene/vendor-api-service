package com.nextgen.gameaggregator.vendor.cosmoplay.config;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchHandler;
import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CosmoPlayVendorConfig extends AbstractVendorConfig {
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String CLASS_NAME = "cosmoplay";
    public static final String GAME_LAUNCHER_SERVICE_NAME = CLASS_NAME + GameLaunchHandler.NAME;

    //get by application.yaml
    @Value("${vendor.cosmoplay.sd-param:#{null}}")
    private String sdParam;

    public CosmoPlayVendorConfig() {
        super(CLASS_NAME);
    }

    public String getSdParam() {
        return sdParam;
    }

    public static String language(String v) {
        return (v == null || v.isBlank()) ? DEFAULT_LANGUAGE : v;
    }

    public static String language(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
    }
}
