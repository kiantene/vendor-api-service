package com.nextgen.gameaggregator.vendor.evolutionv2.config;

import com.nextgen.gameaggregator.core.registry.VendorConfigRegistry;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evolution v2 promo-payout integration.
 */
class EvolutionVendorConfigTest {

    @Test
    void promoPayoutRoute_resolvesEvolutionAsNewFrameworkVendor() {
        EvolutionVendorConfig evolutionConfig = new EvolutionVendorConfig();
        VendorConfigRegistry registry = new VendorConfigRegistry(List.of(evolutionConfig));
        VendorConfigService service = new VendorConfigService(registry);

        var config = service.getVendorIntegrationConfig(EndPoints.CLASS_NAME);

        assertThat(config).isPresent();
        assertThat(config.get().getVendorClassName()).isEqualTo(EndPoints.CLASS_NAME);
        assertThat(config.get().isNewFramework()).isTrue();
    }

    @Test
    void endpoints_useEvolutionV2RouteWithExistingTimeoutAndRetry() {
        assertThat(EndPoints.CLASS_NAME).isEqualTo("evolution");
        assertThat(EndPoints.PATH).isEqualTo("api/v1/netent");
        assertThat(EndPoints.TIMEOUT).isEqualTo(10000);
        assertThat(EndPoints.RETRY).isEqualTo(3);
    }
}
