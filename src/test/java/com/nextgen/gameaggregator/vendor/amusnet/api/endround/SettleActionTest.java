package com.nextgen.gameaggregator.vendor.amusnet.api.endround;

import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * GA-14617 — Amusnet /amusnet/Deposit (settle) duplicate (1100) response missing balance on ResultIdempotent.
 *
 * Bug: on {@link BetResultIdempotentViolationException} the catch block set ErrorCode 1100 (Duplicate) but
 * never {@code vo.setBalance(...)}. Because {@link com.nextgen.gameaggregator.vendor.amusnet.vo.ResponseVo}
 * is {@code @JsonInclude(NON_NULL)}, a null balance is dropped from the XML, producing a Duplicate response
 * with no mandatory {@code <Balance>} — so Amusnet treats the transfer as failed (error 3000) and resends
 * forever. GA-12221 fixed the same defect on the /Withdraw (bet) path only; this covers /Deposit.
 *
 * A jackpot GameId ("999") is used so the heavy verification branches (unsettled-bet lookup, EQ game-code
 * check, game-code regeneration) are skipped, keeping the test focused on the idempotent response mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettleActionTest {

    @Mock private HttpService                httpService;
    @Mock private GameSessionService         gameSessionService;
    @Mock private WalletService              walletService;
    @Mock private VendorLineService          vendorLineService;
    @Mock private UnsettledBetCachingService unsettledBetCachingService;

    @Mock private HttpServletRequest request;
    @Mock private GameSession        gameSession;
    @Mock private BetInformation     betInformation;

    private SettleAction settleAction;
    private MockedStatic<ValidationUtils> validationStatic;

    private static final String DEPOSIT_BODY =
            "<DepositRequest>"
            + "<UserName>vendorUser</UserName>"
            + "<Password>vendorPass</Password>"
            + "<PlayerId>player001</PlayerId>"
            + "<TransferId>transfer-1</TransferId>"
            + "<GameId>999</GameId>"          // jackpot game -> skips unsettled/EQ verification
            + "<GameNumber>round-1</GameNumber>"
            + "<Amount>10.00</Amount>"
            + "<Currency>BRL</Currency>"
            + "<Reason>Win</Reason>"
            + "<PortalCode>PC</PortalCode>"
            + "</DepositRequest>";

    @BeforeEach
    void setUp() throws Exception {
        validationStatic = mockStatic(ValidationUtils.class); // validateRequest + isEquals -> no-op

        // real VendorService so buildResponseVo actually serialises the XML we assert on
        settleAction = new SettleAction(httpService, gameSessionService, walletService,
                new VendorService(), vendorLineService, unsettledBetCachingService);

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        httpRequestLog.setRequestBody(DEPOSIT_BODY);

        when(httpService.start(request)).thenReturn(httpRequestLog);
        when(gameSessionService.getGameSessionByVendorPlayerUsername("player001")).thenReturn(gameSession);
        when(vendorLineService.getCredentialValueByName(any(), any())).thenReturn("x");
    }

    @AfterEach
    void tearDown() {
        validationStatic.close();
    }

    @Test
    @DisplayName("GA-14617: idempotent /Deposit returns Duplicate (1100) WITH the stored balance in the XML")
    void idempotentDeposit_returnsDuplicateWithBalance() throws Exception {
        when(betInformation.getBalance()).thenReturn(new BigDecimal("88.67"));
        BetResultIdempotentViolationException idempotent =
                new BetResultIdempotentViolationException(betInformation);
        when(walletService.processBetResult(any(), any(), any(), any(ResultType.class), any(), any()))
                .thenThrow(idempotent);

        String xml = settleAction.settle(request);

        assertTrue(xml.contains("<ErrorCode>1100</ErrorCode>"), "must be a Duplicate (1100) response: " + xml);
        // Regression guard: before the fix the Balance tag was absent (dropped by @JsonInclude(NON_NULL)).
        assertTrue(xml.contains("<Balance>88</Balance>"), "Duplicate response MUST carry the balance: " + xml);
    }

    @Test
    @DisplayName("GA-14617: non-idempotent /Deposit still returns OK (1000) with balance (sanity)")
    void normalDeposit_returnsOkWithBalance() throws Exception {
        when(walletService.processBetResult(any(), any(), any(), any(ResultType.class), any(), any()))
                .thenReturn(new BigDecimal("120.00"));

        String xml = settleAction.settle(request);

        assertTrue(xml.contains("<ErrorCode>1000</ErrorCode>"), "expected OK response: " + xml);
        assertTrue(xml.contains("<Balance>120</Balance>"), "OK response must carry the balance: " + xml);
        assertFalse(xml.contains("1100"), "should not be a Duplicate: " + xml);
    }
}
