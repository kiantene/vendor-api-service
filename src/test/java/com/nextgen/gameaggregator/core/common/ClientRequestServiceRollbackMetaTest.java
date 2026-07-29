package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.config.properties.WalletServiceProperties;
import com.nextgen.gameaggregator.core.entity.Agent;
import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
import com.nextgen.gameaggregator.core.service.AgentApiCredentialDataService;
import com.nextgen.gameaggregator.core.service.AgentDataService;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.enums.SeamlessType;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackMeta;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers GA-14599: the rollback meta (operator-POV amounts) must reach ONLY the internal transfer
 * wallet. After loadCredential, a seamless-transfer agent's callback URL equals the transfer
 * wallet's URL, which is the gate used by
 * {@link ClientRequestService#stripRollbackMetaForNonTransferWallet}.
 */
class ClientRequestServiceRollbackMetaTest {

    private static final String TW_HOST = "http://transfer-wallet";
    private static final String TW_CALLBACK = TW_HOST + "/seamless"; // == WalletServiceProperties.getCallbackUrl()
    private static final Integer AGENT_ID = 100;

    private ClientRequestService newService() {
        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);
        // The gate is now a boolean (seamless-transfer flag); other deps are unused by the helper.
        return new ClientRequestService(null, null, null, props);
    }

    private static WalletRollbackDto rollbackWithMeta() {
        WalletRollbackDto dto = new WalletRollbackDto();
        RollbackMeta meta = new RollbackMeta();
        meta.setBetAmount(new BigDecimal("10"));
        meta.setWinAmount(new BigDecimal("30")); // win + jackpot combined
        dto.setMeta(meta);
        return dto;
    }

    @Test
    void transferWallet_keepsMeta() {
        ClientRequestService service = newService();
        WalletRollbackDto dto = rollbackWithMeta();

        service.stripRollbackMetaForNonTransferWallet(dto, true); // seamless-transfer agent

        assertNotNull(dto.getMeta(), "meta must be kept for the transfer wallet");
    }

    @Test
    void nonTransferWallet_stripsMeta() {
        ClientRequestService service = newService();
        WalletRollbackDto dto = rollbackWithMeta();

        service.stripRollbackMetaForNonTransferWallet(dto, false); // any other operator

        assertNull(dto.getMeta(), "meta must NOT be sent to normal operators");
    }

    @Test
    void nonRollbackRequest_isNoOp() {
        ClientRequestService service = newService();
        // A non-WalletRollbackDto request must be ignored without error.
        service.stripRollbackMetaForNonTransferWallet(new Object(), false);
    }

    @Test
    void transferWallet_nullMeta_staysNull() {
        ClientRequestService service = newService();
        WalletRollbackDto dto = new WalletRollbackDto(); // no meta set

        service.stripRollbackMetaForNonTransferWallet(dto, true);

        assertNull(dto.getMeta());
    }

    // --- end-to-end: the strip is actually wired into createOperatorApiRequest ----------

    /**
     * Builds the operator request the way the rollback flow does and asserts that a NON-seamless
     * agent's outgoing request carries no meta — i.e. metadata is NEVER sent to a real operator,
     * not just that the helper can strip it.
     */
    @Test
    void createOperatorApiRequest_nonSeamlessAgent_metaNeverSent() {
        Validator validator = mock(Validator.class);
        AgentApiCredentialDataService credentialService = mock(AgentApiCredentialDataService.class);
        AgentDataService agentService = mock(AgentDataService.class);
        when(validator.validate(any())).thenAnswer(i -> Collections.emptySet());

        AgentApiCredential cred = mock(AgentApiCredential.class);
        when(cred.getApiKey()).thenReturn("k");
        when(cred.getApiSecret()).thenReturn("s");
        when(cred.getCallbackUrl()).thenReturn("http://some-operator/api"); // not the transfer wallet
        when(credentialService.getActiveCredential(AGENT_ID)).thenReturn(cred);

        Agent agent = mock(Agent.class);
        when(agent.getSeamlessType()).thenReturn(SeamlessType.SEAMLESS.code); // 1, NOT transfer
        when(agentService.get(AGENT_ID)).thenReturn(agent);

        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);
        ClientRequestService service = new ClientRequestService(validator, credentialService, agentService, props);

        OperatorApiRequest req = service.createOperatorApiRequest(
                "trace", AGENT_ID, "user", "/cash/rollback", rollbackWithMeta(), 1L);

        WalletRollbackDto body = (WalletRollbackDto) req.getBody();
        assertNull(body.getMeta(), "meta must NEVER be sent to a non-transfer-wallet operator");
    }

    @Test
    void createOperatorApiRequest_seamlessTransferAgent_metaSent() {
        Validator validator = mock(Validator.class);
        AgentApiCredentialDataService credentialService = mock(AgentApiCredentialDataService.class);
        AgentDataService agentService = mock(AgentDataService.class);
        when(validator.validate(any())).thenAnswer(i -> Collections.emptySet());

        AgentApiCredential cred = mock(AgentApiCredential.class);
        when(cred.getApiKey()).thenReturn("k");
        when(cred.getApiSecret()).thenReturn("s");
        // loadCredential routes a seamless-transfer agent's callback to the transfer wallet URL.
        when(cred.getCallbackUrl()).thenReturn(TW_CALLBACK);
        when(credentialService.getActiveCredential(AGENT_ID)).thenReturn(cred);

        Agent agent = mock(Agent.class);
        when(agent.getSeamlessType()).thenReturn(SeamlessType.SEAMLESS_TRANSFER.code); // 2, transfer wallet
        when(agentService.get(AGENT_ID)).thenReturn(agent);

        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);
        ClientRequestService service = new ClientRequestService(validator, credentialService, agentService, props);

        OperatorApiRequest req = service.createOperatorApiRequest(
                "trace", AGENT_ID, "user", "/cash/rollback", rollbackWithMeta(), 1L);

        WalletRollbackDto body = (WalletRollbackDto) req.getBody();
        assertNotNull(body.getMeta(), "meta must be sent to the transfer wallet");
        verify(cred).setCallbackUrl(TW_CALLBACK); // seamless-transfer detection routed to the transfer wallet
    }

    @Test
    void createOperatorApiRequest_agentOnCredential_singleLookup_noSecondAgentFetch() {
        Validator validator = mock(Validator.class);
        AgentApiCredentialDataService credentialService = mock(AgentApiCredentialDataService.class);
        AgentDataService agentService = mock(AgentDataService.class);
        when(validator.validate(any())).thenAnswer(i -> Collections.emptySet());

        Agent agent = mock(Agent.class);
        when(agent.getSeamlessType()).thenReturn(SeamlessType.SEAMLESS_TRANSFER.code);

        AgentApiCredential cred = mock(AgentApiCredential.class);
        when(cred.getApiKey()).thenReturn("k");
        when(cred.getApiSecret()).thenReturn("s");
        when(cred.getCallbackUrl()).thenReturn(TW_CALLBACK);
        when(cred.getAgent()).thenReturn(agent); // credential carries the agent => no second lookup
        when(credentialService.getActiveCredential(AGENT_ID)).thenReturn(cred);

        WalletServiceProperties props = new WalletServiceProperties();
        props.setHost(TW_HOST);
        ClientRequestService service = new ClientRequestService(validator, credentialService, agentService, props);

        OperatorApiRequest req = service.createOperatorApiRequest(
                "trace", AGENT_ID, "user", "/cash/rollback", rollbackWithMeta(), 1L);

        WalletRollbackDto body = (WalletRollbackDto) req.getBody();
        assertNotNull(body.getMeta(), "transfer-wallet meta resolved from the credential's agent");
        verify(agentService, never()).get(any()); // no redundant agent cache lookup
    }
}
