package com.nextgen.gameaggregator.vendor.bglive.api.transfer;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private AgentPlayerService agentPlayerService;
    @Mock private VendorLineService vendorLineService;
    @Mock private GameSessionService gameSessionService;
    @Mock private HttpService httpService;
    @Mock private VendorService vendorService;
    @Mock private VendorGameService vendorGameService;
    @Mock private WalletRequestService walletRequestService;
    @Mock private OperatorWalletService operatorWalletService;
    @Mock private RequestIdempotentLogService requestIdempotentLogService;

    @InjectMocks
    private TransferService transferService;

    private HttpRequestLog httpRequestLog;
    private GameSession gameSession;
    private TransferDto transferDto;
    private ParamsDto paramsDto;
    private WalletRequest walletRequest;

    private static final String GAME_ID = "GAME_BG_001";
    private static final Integer VENDOR_ID = 5;
    private static final String LOGIN_ID = "player123";
    private static final String SN_CODE = "SN12345";
    private static final String SECRET_KEY = "SECRET_KEY";
    private static final String BIZ_ID = "BIZ_999";

    @BeforeEach
    void setUp() {
        httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("100");
        httpRequestLog.setRequestBody("{\"id\":\"1\",\"method\":\"open.operator.user.transfer\",\"params\":{\"gameId\":\"" + GAME_ID + "\",\"loginId\":\"" + LOGIN_ID + "\",\"bizId\":\"" + BIZ_ID + "\"}}");

        paramsDto = new ParamsDto();
        paramsDto.setLoginId(LOGIN_ID);
        paramsDto.setSn(SN_CODE);
        paramsDto.setRandom("rand123");
        paramsDto.setAmount(new BigDecimal("10.00"));
        paramsDto.setSign("valid_sign");
        paramsDto.setGameId(GAME_ID);
        paramsDto.setBizId(BIZ_ID);

        transferDto = new TransferDto();
        transferDto.setId("1");
        transferDto.setMethod("open.operator.user.transfer");
        transferDto.setParamsDto(paramsDto);

        gameSession = new GameSession();
        gameSession.setVendorLineId(10);
        gameSession.setVendorId(VENDOR_ID);
        gameSession.setAgentPlayerId(20L);
        gameSession.setVendorPlayerUsername(LOGIN_ID);
        gameSession.setVendorGameCode("FALLBACK_SESSION_GAME_CODE");

        walletRequest = new WalletRequest();
        walletRequest.setBalanceAfter(new BigDecimal("100.00"));
    }

    private void mockVerificationSuccess() throws Exception {
        when(gameSessionService.getGameSessionByVendorPlayerUsername(LOGIN_ID)).thenReturn(gameSession);
        when(vendorLineService.getCredentialValueByName(10, Credentials.SN_CODE)).thenReturn(SN_CODE);
        when(vendorLineService.getCredentialValueByName(10, Credentials.API_KEY)).thenReturn(SECRET_KEY);
    }

    @Test
    @DisplayName("transfer() returns system error response when game code is invalid or disabled")
    void transfer_WhenGameInvalidOrDisabled_ShouldReturnSystemError() throws Exception {
        try (MockedStatic<HttpService> httpServiceMock = mockStatic(HttpService.class);
             MockedStatic<WalletRequestService> walletReqMock = mockStatic(WalletRequestService.class);
             MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class)) {

            httpServiceMock.when(() -> HttpService.convertJsonToDto(any(), eq(TransferDto.class))).thenReturn(transferDto);
            walletReqMock.when(() -> WalletRequestService.init(any())).thenReturn(walletRequest);
            vendorServiceMock.when(() -> VendorService.encryptBetMd5Key(any(), any(), any(), any(), any())).thenReturn("valid_sign");

            mockVerificationSuccess();
            doThrow(new GameNotSupportedException("Game is disabled or not supported"))
                    .when(vendorGameService).getByVendorGameCodeAndVendorId(eq(GAME_ID), eq(VENDOR_ID));

            CommonVo response = transferService.transfer(httpRequestLog);

            assertNotNull(response.getError());
            assertEquals(ResponseCodes.SYSTEM_ERROR.code, response.getError().getCode());
            verify(operatorWalletService, never()).betDebit(any());
            verify(operatorWalletService, never()).betCredit(any());
        }
    }

    @Test
    @DisplayName("transfer() proceeds to wallet processing when game is active")
    void transfer_WhenGameActive_ShouldProceedToWalletProcessing() throws Exception {
        try (MockedStatic<HttpService> httpServiceMock = mockStatic(HttpService.class);
             MockedStatic<WalletRequestService> walletReqMock = mockStatic(WalletRequestService.class);
             MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class)) {

            httpServiceMock.when(() -> HttpService.convertJsonToDto(any(), eq(TransferDto.class))).thenReturn(transferDto);
            walletReqMock.when(() -> WalletRequestService.init(any())).thenReturn(walletRequest);
            vendorServiceMock.when(() -> VendorService.encryptBetMd5Key(any(), any(), any(), any(), any())).thenReturn("valid_sign");

            mockVerificationSuccess();

            VendorGame mockVendorGame = new VendorGame();
            when(vendorGameService.getByVendorGameCodeAndVendorId(eq(GAME_ID), eq(VENDOR_ID))).thenReturn(mockVendorGame);

            when(requestIdempotentLogService.checkExists(any(TransferDto.class), anyString())).thenReturn(null);
            when(vendorService.calculateResultType(any(), any(), any(), anyBoolean())).thenReturn(ResultType.WIN);
            when(operatorWalletService.betCredit(any())).thenReturn(walletRequest);

            CommonVo response = transferService.transfer(httpRequestLog);

            verify(vendorGameService, times(1)).getByVendorGameCodeAndVendorId(eq(GAME_ID), eq(VENDOR_ID));
            verify(operatorWalletService, times(1)).betCredit(any());
            assertEquals(walletRequest.getBalanceAfter(), response.getResult());
            assertEquals(GAME_ID, walletRequest.getVendorGameCode());
        }
    }

    @Test
    @DisplayName("transfer() routes to betDebit and maps game code when amount is negative")
    void transfer_WhenAmountIsNegative_ShouldProceedToBetDebitAndMapGameCode() throws Exception {
        try (MockedStatic<HttpService> httpServiceMock = mockStatic(HttpService.class);
             MockedStatic<WalletRequestService> walletReqMock = mockStatic(WalletRequestService.class);
             MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class)) {

            // Setup negative transfer amount (-10.00)
            paramsDto.setAmount(new BigDecimal("-10.00"));

            httpServiceMock.when(() -> HttpService.convertJsonToDto(any(), eq(TransferDto.class))).thenReturn(transferDto);
            walletReqMock.when(() -> WalletRequestService.init(any())).thenReturn(walletRequest);
            vendorServiceMock.when(() -> VendorService.encryptBetMd5Key(any(), any(), any(), any(), any())).thenReturn("valid_sign");

            mockVerificationSuccess();

            VendorGame mockVendorGame = new VendorGame();
            when(vendorGameService.getByVendorGameCodeAndVendorId(eq(GAME_ID), eq(VENDOR_ID))).thenReturn(mockVendorGame);

            when(requestIdempotentLogService.checkExists(any(TransferDto.class), anyString())).thenReturn(null);
            // REMOVED: calculateResultType stubbing was unused on the debit path
            when(operatorWalletService.betDebit(any())).thenReturn(walletRequest);

            CommonVo response = transferService.transfer(httpRequestLog);

            // Verify betDebit execution path and symmetric dataDebitMapper outcome
            verify(operatorWalletService, times(1)).betDebit(any());
            verify(operatorWalletService, never()).betCredit(any());
            assertEquals(walletRequest.getBalanceAfter(), response.getResult());
            assertEquals(GAME_ID, walletRequest.getVendorGameCode());
        }
    }
}
