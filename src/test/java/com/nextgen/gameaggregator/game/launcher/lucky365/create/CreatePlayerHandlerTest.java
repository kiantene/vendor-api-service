package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;

/**
 * ONEAPI-244/245: Lucky365 rejects CreatePlayer when PlayerName contains a hyphen.
 * PlayerName now reuses the vendor username (already hyphen-free) instead of the
 * agent username, so the launch succeeds regardless of the agent username's format.
 */
@ExtendWith(MockitoExtension.class)
class CreatePlayerHandlerTest {

    private static final String SERIAL_NUM_VALUE = "SN001";
    private static final String SECRET_KEY_VALUE = "secret";
    private static final String VENDOR_PLAYER_USERNAME = "abc123";

    @Mock
    private VendorCredentialUtils credentialUtils;
    @Mock
    private VendorCredentialAccessor credentialAccessor;

    private CreatePlayerHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreatePlayerHandler(credentialUtils);

        lenient().when(credentialUtils.of(anyMap())).thenReturn(credentialAccessor);
        lenient().when(credentialAccessor.getValue("serialNumber")).thenReturn(SERIAL_NUM_VALUE);
        lenient().when(credentialAccessor.getValue("secretKey")).thenReturn(SECRET_KEY_VALUE);
    }

    private GameLaunchContext buildContext(String agentPlayerUsername) {
        return GameLaunchContext.builder()
                .token("token-1")
                .vendorPlayerUsername(VENDOR_PLAYER_USERNAME)
                .agentPlayerUsername(agentPlayerUsername)
                .vendorCredentials(new HashMap<>())
                .build();
    }

    @Test
    void playerNameUsesVendorUsername_evenWhenAgentUsernameHasHyphens() {
        GameLaunchContext context = buildContext("01a016b9-399e-72bb-8eb9-1dcc77e28bc5");

        CreatePlayerRequest request = handler.buildRequestBody(context);

        assertThat(request.getPlayerName()).isEqualTo(VENDOR_PLAYER_USERNAME);
        assertThat(request.getPlayerCode()).isEqualTo(VENDOR_PLAYER_USERNAME);
    }

    @Test
    void playerNameAndPlayerCode_alwaysMatchVendorUsername() {
        GameLaunchContext context = buildContext(null);

        CreatePlayerRequest request = handler.buildRequestBody(context);

        assertThat(request.getPlayerName()).isEqualTo(VENDOR_PLAYER_USERNAME);
        assertThat(request.getPlayerCode()).isEqualTo(VENDOR_PLAYER_USERNAME);
    }
}
