package com.nextgen.gameaggregator.core.vendor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.entity.VendorConfig;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.evoplay.config.EvoplayConfig;
import com.nextgen.gameaggregator.vendor.koolbet.config.KoolbetConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Locks the code defaults for the Evoplay and Koolbet migration vendors.
 *
 * <p>The production {@code vendor_config} documents only carry
 * {@code callbackRoutingEnabled}; {@code walletServiceLegacyEnabled} is absent, so
 * {@link AbstractVendorConfig#updateFromDB} leaves the code default in place and the
 * code default is the effective production value. These assertions exist so that
 * changing either default is a deliberate act rather than a silent behaviour change.</p>
 */
class MigrationVendorDefaultsTest {

    private static final String EVOPLAY_DB_DOC = """
            {"className":"evoplay","integrationConfig":{"transactionHistoryEnabled":true,"callbackRoutingEnabled":true}}""";

    /** Deliberately differs from the code defaults on both flags. */
    private static final String EVOPLAY_DB_DOC_OVERRIDES = """
            {"className":"evoplay","integrationConfig":{"callbackRoutingEnabled":false,"walletServiceLegacyEnabled":true}}""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EvoplayConfig evoplayConfig() {
        return new EvoplayConfig(mock(MigrationRoundDataService.class));
    }

    private KoolbetConfig koolbetConfig() {
        return new KoolbetConfig(mock(MigrationRoundDataService.class), new ObjectMapper());
    }

    @Test
    void evoplayDefaultsToV2RoutingOnTheNewWalletPath() {
        assertThat(evoplayConfig().isCallbackRoutingEnabled()).isTrue();
        assertThat(evoplayConfig().isWalletServiceLegacyEnabled()).isFalse();
    }

    @Test
    void koolbetDefaultsToV2RoutingOnTheNewWalletPath() {
        assertThat(koolbetConfig().isCallbackRoutingEnabled()).isTrue();
        assertThat(koolbetConfig().isWalletServiceLegacyEnabled()).isFalse();
    }

    @Test
    void absentWalletServiceLegacyKeyKeepsTheCodeDefault() throws Exception {
        VendorConfig fromDB = objectMapper.readValue(EVOPLAY_DB_DOC, VendorConfig.class);
        assertThat(fromDB.getIntegrationConfig().getWalletServiceLegacyEnabled()).isNull();

        EvoplayConfig config = evoplayConfig();
        config.updateFromDB(fromDB);

        assertThat(config.isWalletServiceLegacyEnabled()).isFalse();
    }

    /**
     * Both values differ from the code defaults, so each assertion fails if
     * {@link AbstractVendorConfig#updateFromDB} stops applying that field.
     */
    @Test
    void presentDbValuesOverrideTheCodeDefaults() throws Exception {
        VendorConfig fromDB = objectMapper.readValue(EVOPLAY_DB_DOC_OVERRIDES, VendorConfig.class);

        EvoplayConfig config = evoplayConfig();
        config.updateFromDB(fromDB);

        assertThat(config.isCallbackRoutingEnabled()).isFalse();
        assertThat(config.isWalletServiceLegacyEnabled()).isTrue();
    }
}
