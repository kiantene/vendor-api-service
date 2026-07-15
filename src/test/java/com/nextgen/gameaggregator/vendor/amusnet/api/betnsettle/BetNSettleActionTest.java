package com.nextgen.gameaggregator.vendor.amusnet.api.betnsettle;

import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * GA-14617 — Amusnet /amusnet/WithdrawAndDeposit (bet-n-settle) duplicate (1100) response missing balance.
 *
 * Same defect as {@link com.nextgen.gameaggregator.vendor.amusnet.api.endround.SettleActionTest}: the
 * {@link BetResultIdempotentViolationException} catch block set ErrorCode 1100 without a balance, and
 * {@code @JsonInclude(NON_NULL)} dropped the null {@code <Balance>} from the XML. GA-12221 patched only the
 * /Withdraw path; this covers /WithdrawAndDeposit.
 *
 * A spy VendorService stubs the game-code lookups while keeping the real {@code buildResponseVo} so the
 * asserted XML is actually serialised.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BetNSettleActionTest {

    @Mock private HttpService        httpService;
    @Mock private GameSessionService gameSessionService;
    @Mock private WalletService      walletService;
    @Mock private ValidationService  validationService;
    @Mock private VendorLineService  vendorLineService;

    @Mock private HttpServletRequest request;
    @Mock private GameSession        gameSession;
    @Mock private BetInformation     betInformation;

    private BetNSettleAction betNSettleAction;
    private VendorService    vendorService;
    private MockedStatic<ValidationUtils> validationStatic;

    private static final String BODY =
            "<WithdrawAndDepositRequest>"
            + "<UserName>vendorUser</UserName>"
            + "<Password>vendorPass</Password>"
            + "<PlayerId>player001</PlayerId>"
            + "<TransferId>transfer-1</TransferId>"
            + "<GameId>123</GameId>"
            + "<GameNumber>round-1</GameNumber>"
            + "<Amount>10.00</Amount>"
            + "<WinAmount>5.00</WinAmount>"
            + "<Currency>BRL</Currency>"
            + "<Reason>Win</Reason>"
            + "<PortalCode>PC</PortalCode>"
            + "</WithdrawAndDepositRequest>";

    @BeforeEach
    void setUp() throws Exception {
        validationStatic = mockStatic(ValidationUtils.class); // validateRequest + isEquals -> no-op

        vendorService = spy(new VendorService()); // real buildResponseVo, stubbed lookups
        // doReturn form: avoids invoking the real spy methods during stubbing
        doReturn(gameSession).when(vendorService).verifyAndRegenerateNewVendorGameCodeForGameSession(any(), any());
        doReturn("PC").when(vendorService).checkGameCodeIsOpenEQGame(any(), any(), any(), any());

        betNSettleAction = new BetNSettleAction(httpService, gameSessionService, walletService,
                validationService, vendorService, vendorLineService);

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        httpRequestLog.setRequestBody(BODY);

        when(httpService.start(request)).thenReturn(httpRequestLog);
        when(gameSessionService.getGameSessionByVendorPlayerUsername("player001")).thenReturn(gameSession);
        when(vendorLineService.getCredentialValueByName(any(), any())).thenReturn("x");
    }

    @AfterEach
    void tearDown() {
        validationStatic.close();
    }

    @Test
    @DisplayName("GA-14617: idempotent /WithdrawAndDeposit returns Duplicate (1100) WITH the stored balance")
    void idempotentBetNSettle_returnsDuplicateWithBalance() throws Exception {
        when(betInformation.getBalance()).thenReturn(new BigDecimal("88.67"));
        BetResultIdempotentViolationException idempotent =
                new BetResultIdempotentViolationException(betInformation);
        when(walletService.processBetResult(any(), any(), any(), any(ResultType.class), any(), any()))
                .thenThrow(idempotent);

        String xml = betNSettleAction.betResult(request);

        assertTrue(xml.contains("<ErrorCode>1100</ErrorCode>"), "must be a Duplicate (1100) response: " + xml);
        assertTrue(xml.contains("<Balance>88</Balance>"), "Duplicate response MUST carry the balance: " + xml);
    }
}
