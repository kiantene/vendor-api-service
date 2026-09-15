package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.entity.VendorGame;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorGameDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verification on {@code /transactions/promoWin}, and how each failure maps onto Aviatrix's error table.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromoWinActionTest {

    private static final String PAYLOAD = """
            {"cid":"brand-1","sessionToken":"token-1","playerId":"player-1","productId":"nft-aviatrix",
             "txId":"tx-1","amount":333,"currency":"USD","promo":{"type":"tournament"}}""";

    @Mock private HttpService httpService;
    @Mock private GameSessionService gameSessionService;
    @Mock private VendorLineService vendorLineService;
    @Mock private VendorPlayerDataService vendorPlayerDataService;
    @Mock private VendorGameDataService vendorGameDataService;
    @Mock private VendorCurrencyDataService vendorCurrencyDataService;
    @Mock private AviatrixPromoPayoutService promoPayoutService;

    @InjectMocks private PromoWinAction action;

    @BeforeEach
    void stubTheHappyPath() throws Exception {
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        httpRequestLog.setRequestBody(PAYLOAD);
        when(httpService.start(any(HttpServletRequest.class))).thenReturn(httpRequestLog);

        VendorPlayer vendorPlayer = mock(VendorPlayer.class);
        when(vendorPlayer.getVendorLineId()).thenReturn(7);
        when(vendorPlayer.getVendorId()).thenReturn(96);
        when(vendorPlayer.getCurrencyId()).thenReturn(1);
        when(vendorPlayerDataService.getByUsername("player-1")).thenReturn(vendorPlayer);

        when(vendorLineService.getCredentialValueByName(eq(7), any())).thenReturn("brand-1");

        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("player-1");
        when(gameSessionService.verifyToken("token-1")).thenReturn(gameSession);

        when(vendorGameDataService.getByVendorGameCodeAndVendorId(eq("nft-aviatrix"), anyInt()))
                .thenReturn(mock(VendorGame.class));

        VendorCurrency vendorCurrency = mock(VendorCurrency.class);
        when(vendorCurrency.getVendorCurrencyCode()).thenReturn("USD");
        when(vendorCurrencyDataService.getByVendorIdAndCurrencyId(anyInt(), anyInt())).thenReturn(vendorCurrency);

        ResponseVo paid = new ResponseVo();
        paid.setBalance(BigInteger.valueOf(5000));
        paid.setCreatedAt("2026-08-20T10:00:00+08:00");
        when(promoPayoutService.payout(any(), any())).thenReturn(paid);
    }

    @Test
    void paysOutAVerifiedPromoWin() {
        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance()).isEqualTo(BigInteger.valueOf(5000));
        assertThat(response.getBody().getCreatedAt()).isNotBlank();
        verify(promoPayoutService).payout(any(), any());
    }

    @Test
    void rejectsASessionTokenBelongingToAnotherPlayer() throws Exception {
        GameSession someoneElse = new GameSession();
        someoneElse.setVendorPlayerUsername("player-2");
        when(gameSessionService.verifyToken("token-1")).thenReturn(someoneElse);

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.PLAYER_NOT_FOUND);
        verify(promoPayoutService, never()).payout(any(), any());
    }

    /**
     * A promo can be granted to a player who never launched a game — a daily bonus, as in OAS-5107 — so an
     * unresolvable session must not fail the payout.
     */
    @Test
    void paysOutWhenNoSessionResolvesForTheToken() throws Exception {
        when(gameSessionService.verifyToken("token-1")).thenThrow(new AuthenticationException());

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(promoPayoutService).payout(any(), any());
    }

    @Test
    void rejectsAnUnknownPlayer() {
        when(vendorPlayerDataService.getByUsername("player-1"))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", "player-1"));

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.PLAYER_NOT_FOUND);
        verify(promoPayoutService, never()).payout(any(), any());
    }

    @Test
    void rejectsAMismatchedCid() throws Exception {
        when(vendorLineService.getCredentialValueByName(eq(7), any())).thenReturn("a-different-brand");

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.PLATFORM_NOT_FOUND);
        verify(promoPayoutService, never()).payout(any(), any());
    }

    @Test
    void rejectsAProductThisVendorDoesNotOffer() {
        when(vendorGameDataService.getByVendorGameCodeAndVendorId(eq("nft-aviatrix"), anyInt()))
                .thenThrow(new EntityNotFoundException(VendorGame.class, "vendorGameCode", "nft-aviatrix"));

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.PRODUCT_NOT_FOUND);
        verify(promoPayoutService, never()).payout(any(), any());
    }

    @Test
    void rejectsACurrencyThePlayerDoesNotHold() {
        VendorCurrency eur = mock(VendorCurrency.class);
        when(eur.getVendorCurrencyCode()).thenReturn("EUR");
        when(vendorCurrencyDataService.getByVendorIdAndCurrencyId(anyInt(), anyInt())).thenReturn(eur);

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.INVALID_PLAYER_CURRENCY);
        verify(promoPayoutService, never()).payout(any(), any());
    }

    /** A genuine replay: the recorded balance is echoed back as a success. */
    @Test
    void echoesTheRecordedBalanceOnAReplayedTransaction() {
        RequestIdempotentLog log = new RequestIdempotentLog();
        log.setCurrency("USD");
        log.setBalance(new java.math.BigDecimal("77.00"));
        when(promoPayoutService.payout(any(), any()))
                .thenThrow(new DuplicateRequestException("tx-1 already processed", log));

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBalance()).isEqualTo(BigInteger.valueOf(7700));
        assertThat(response.getBody().getCreatedAt()).isNotBlank();
    }

    /**
     * A duplicate with no recorded currency means the first attempt died before paying out. Reporting
     * success would hand Aviatrix a settled payout that never happened.
     */
    @Test
    void refusesToReportSuccessForAnUnenrichedIdempotencyRow() {
        RequestIdempotentLog log = new RequestIdempotentLog();  // never enriched: no currency, no balance
        when(promoPayoutService.payout(any(), any()))
                .thenThrow(new DuplicateRequestException("tx-1 already processed", log));

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.UNKNOWN_ERROR);
        assertThat(response.getBody().getBalance()).isNull();
    }

    /**
     * An unrecognised bonusId is the caller's problem, not ours. A 500 here would spend Aviatrix's entire
     * retry chain on a payout that can never succeed.
     */
    @Test
    void reportsAnUnresolvableCampaignAsABadRequestRatherThanAFault() {
        when(promoPayoutService.payout(any(), any()))
                .thenThrow(new EntityNotFoundException(Campaign.class, "resolveCampaign", "bonus-nope"));

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.INVALID_TRANSACTION);
    }

    /** The OAS-5107 failure: a promo object the DTO could not deserialize must be a 400, not a 500. */
    @Test
    void rejectsAnUnparseableBodyAsABadRequest() {
        HttpRequestLog malformed = new HttpRequestLog();
        malformed.setId("trace-1");
        malformed.setRequestBody(PAYLOAD.replace("\"amount\":333", "\"amount\":{}"));
        when(httpService.start(any(HttpServletRequest.class))).thenReturn(malformed);

        ResponseEntity<ResponseVo> response = action.promoWin(mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseCodes.INVALID_REQUEST);
        verify(promoPayoutService, never()).payout(any(), any());
    }
}
