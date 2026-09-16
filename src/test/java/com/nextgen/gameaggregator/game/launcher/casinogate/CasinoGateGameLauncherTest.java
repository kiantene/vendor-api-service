package com.nextgen.gameaggregator.game.launcher.casinogate;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Credentials;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CasinoGateGameLauncherTest {

    private final CasinoGateGameLauncher launcher = new CasinoGateGameLauncher(new VendorCredentialUtils());

    @Test
    void buildRequestBodyUsesCredentialCasinoIdAndContextLobbyUrl() {
        GameLaunchContext context = GameLaunchContext.builder()
                .vendorCredentials(Map.of(Credentials.CASINO_ID, credential("casino-credential")))
                .vendorGameCode("game.with.prefix")
                .vendorCurrencyCode("EUR")
                .vendorLanguageCode("en")
                .platformId(1)
                .lobbyUrl("https://operator.example/lobby")
                .ipAddress("8.8.8.8")
                .token("session-token")
                .vendorPlayerUsername("player-1")
                .build();

        GameLaunchRequest request = launcher.buildRequestBody(context);

        assertThat(request.getCasinoId()).isEqualTo("casino-credential");
        assertThat(request.getCashierUrl()).isEqualTo("https://operator.example/lobby");
        assertThat(request.getLobbyUrl()).isEqualTo("https://operator.example/lobby");
        assertThat(request.getGameId()).isEqualTo("game.with.prefix");
    }

    private VendorLineCredential credential(String value) {
        VendorLineCredential credential = new VendorLineCredential();
        credential.setValue(value);
        return credential;
    }
}
