package com.nextgen.gameaggregator.vendor.hacksaw.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The vendor only triggers a rollback on statusCode 1 (GENERAL_OR_SERVER_ERROR), not
 * 11 (GENERAL_ERROR). Any operator-side failure must map to 1 so a bet isn't silently
 * lost; other error types are unaffected and still map to 11.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String SECRET = "shared-secret";
    private static final String EXTERNAL_PLAYER_ID = "player-abc-1";
    private static final String EXTERNAL_SESSION_ID = "session-abc-1";
    private static final Integer VENDOR_LINE_ID = 501;

    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private VendorLineService vendorLineService;
    @Mock
    private WalletService walletService;
    @Mock
    private HttpService httpService;
    @Mock
    private VendorService vendorService;
    @Mock
    private ValidationService validationService;
    @Mock
    private RequestIdempotentLogService requestIdempotentLogService;

    @InjectMocks
    private TransactionService transactionService;

    private GameSession gameSession;

    @BeforeEach
    void setUp() throws Exception {
        gameSession = new GameSession();
        gameSession.setVendorLineId(VENDOR_LINE_ID);
        gameSession.setVendorCurrencyCode("NZD");
        gameSession.setVendorPlayerUsername(EXTERNAL_PLAYER_ID);

        lenient().when(requestIdempotentLogService.checkExists(any(BetResultData.class), any())).thenReturn(null);
        lenient().when(gameSessionService.verifyToken(EXTERNAL_SESSION_ID)).thenReturn(gameSession);
        lenient().when(vendorLineService.getCredentialValueByName(any(), any())).thenReturn(SECRET);
    }

    private TransactionDto buildDto() {
        TransactionDto dto = new TransactionDto();
        dto.setAction("Bet");
        dto.setSecret(SECRET);
        dto.setGameId(1310);
        dto.setRoundId(13002775860685L);
        dto.setExternalPlayerId(EXTERNAL_PLAYER_ID);
        dto.setAmount(5000L);
        dto.setCurrency("NZD");
        dto.setGameSessionId(1L);
        dto.setExternalSessionId(EXTERNAL_SESSION_ID);
        dto.setTransactionId(1L);
        return dto;
    }

    private HttpRequestLog buildRequestLog(TransactionDto dto) throws Exception {
        HttpRequestLog log = new HttpRequestLog();
        log.setRequestBody(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dto));
        return log;
    }

    @Test
    void operatorTimeout_mapsToGeneralOrServerError_soVendorTriggersRollback() throws Exception {
        when(walletService.processBet(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidOperatorResponseException(Status.SC_OPERATOR_TIMEOUT.code));

        ResponseVo vo = transactionService.transaction(buildRequestLog(buildDto()), "trace-1");

        assertThat(vo.getResponseCodes()).isEqualTo(ResponseCodes.GENERAL_OR_SERVER_ERROR);
        assertThat(ResponseCodes.GENERAL_OR_SERVER_ERROR.statusCode).isEqualTo(1);
    }

    @Test
    void otherOperatorError_stillMapsToGeneralOrServerError() throws Exception {
        // Any operator-side failure maps here, not just a timeout.
        when(walletService.processBet(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidOperatorResponseException(Status.SC_UNKNOWN_ERROR.code));

        ResponseVo vo = transactionService.transaction(buildRequestLog(buildDto()), "trace-2");

        assertThat(vo.getResponseCodes()).isEqualTo(ResponseCodes.GENERAL_OR_SERVER_ERROR);
    }

    @Test
    void unrelatedError_stillMapsToGeneralError_unaffectedByTheSplit() throws Exception {
        when(walletService.processBet(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidAgentApiCredentialException());

        ResponseVo vo = transactionService.transaction(buildRequestLog(buildDto()), "trace-3");

        assertThat(vo.getResponseCodes()).isEqualTo(ResponseCodes.GENERAL_ERROR);
        assertThat(ResponseCodes.GENERAL_ERROR.statusCode).isEqualTo(11);
    }
}
