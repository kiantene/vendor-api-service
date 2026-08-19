package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.UnsettledBetCachingService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.evoplay.api.authenticate.InitService;
import com.nextgen.gameaggregator.vendor.evoplay.api.balanceIncrease.BalanceIncreaseService;
import com.nextgen.gameaggregator.vendor.evoplay.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.evoplay.api.endround.WinService;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayBalanceIncreaseValidator;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutRequest;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutRequestFactory;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutResult;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundPayoutService;
import com.nextgen.gameaggregator.vendor.evoplay.api.freeround.EvoplayFreeRoundResponseAdapter;
import com.nextgen.gameaggregator.vendor.evoplay.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.evoplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackActionDecodedWinRoutingTest {
    private static final String TOKEN = "ee3b61a2-7c73-4f5b-9d06-02ce22b26a7d";
    private static final String PROJECT_ID = "12345";
    private static final String SIGNATURE_KEY = "signature-key";

    @Mock
    private HttpService httpService;
    @Mock
    private InitService initService;
    @Mock
    private BetService betService;
    @Mock
    private WinService winService;
    @Mock
    private RefundService refundService;
    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private VendorLineService vendorLineService;
    @Mock
    private WalletService walletService;
    @Mock
    private BalanceIncreaseService balanceIncreaseService;
    @Mock
    private UnsettledBetCachingService unsettledBetCachingService;
    @Mock
    private MigrationRoundDataService migrationRoundDataService;
    @Mock
    private EvoplayBalanceIncreaseValidator evoplayBalanceIncreaseValidator;
    @Mock
    private EvoplayFreeRoundPayoutService evoplayFreeRoundPayoutService;
    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private CallbackAction callbackAction;

    private HttpRequestLog httpRequestLog;
    private GameSession gameSession;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(callbackAction, "evoplayFreeRoundPayoutRequestFactory", new EvoplayFreeRoundPayoutRequestFactory());
        ReflectionTestUtils.setField(callbackAction, "evoplayFreeRoundResponseAdapter", new EvoplayFreeRoundResponseAdapter());

        httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        when(httpService.start(servletRequest)).thenReturn(httpRequestLog);

        gameSession = new GameSession();
        gameSession.setToken(TOKEN);
        gameSession.setVendorToken(TOKEN);
        gameSession.setVendorLineId(10);
        gameSession.setVendorCurrencyCode("THB");
        gameSession.setVendorGameCode("5951");
        gameSession.setVendorPlayerUsername("vendor-player-1");
        when(gameSessionService.verifyToken(TOKEN)).thenReturn(gameSession);

        when(vendorLineService.getCredentialValueByName(10, Credentials.PROJ_ID)).thenReturn(PROJECT_ID);
        when(vendorLineService.getCredentialValueByName(10, Credentials.KEY)).thenReturn(SIGNATURE_KEY);
    }

    @Test
    void callback_routesAwardedFreeSpinWinToPromoPayout() throws Exception {
        httpRequestLog.setRequestBody(signed(winBody(
                "pbz1isnzctkhb",
                "7857169989",
                "25",
                freeSpinDetails("25", "3", "7")
        )));
        when(evoplayFreeRoundPayoutService.payout(any(EvoplayFreeRoundPayoutRequest.class), same(httpRequestLog)))
                .thenReturn(EvoplayFreeRoundPayoutResult.builder().balance(new BigDecimal("217996.02")).currency("THB").build());

        ResponseVo response = callbackAction.callback(servletRequest);

        ArgumentCaptor<EvoplayFreeRoundPayoutRequest> payoutRequest = ArgumentCaptor.forClass(EvoplayFreeRoundPayoutRequest.class);
        verify(gameSessionService).verifyToken(TOKEN);
        verify(gameSessionService, never()).getGameSessionByVendorPlayerUsername(null);
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("7857169989");
        verify(evoplayFreeRoundPayoutService).payout(payoutRequest.capture(), same(httpRequestLog));
        verify(winService, never()).win(any(), any(), any(), any());
        assertThat(payoutRequest.getValue().getId()).isEqualTo("pbz1isnzctkhb");
        assertThat(payoutRequest.getValue().getUserId()).isEqualTo("vendor-player-1");
        assertThat(payoutRequest.getValue().getEventId()).isEqualTo("019fcb9fdfab7bbaaff6fe6da7943c7d");
        assertThat(payoutRequest.getValue().getAmount()).isEqualByComparingTo("25");
        assertThat(payoutRequest.getValue().isPlayerUuidCampaignLookup()).isTrue();
        assertThat(response.getData().getBalance()).isEqualByComparingTo("217996.02");
    }

    @Test
    void callback_routesZeroAmountFreeSpinWinToPromoPayout() throws Exception {
        httpRequestLog.setRequestBody(signed(winBody(
                "8ut0i1v2itwps",
                "7857169864",
                "0",
                freeSpinDetails("0", "4", "6")
        )));
        when(evoplayFreeRoundPayoutService.payout(any(EvoplayFreeRoundPayoutRequest.class), same(httpRequestLog)))
                .thenReturn(EvoplayFreeRoundPayoutResult.builder().balance(new BigDecimal("217996.02")).currency("THB").build());

        callbackAction.callback(servletRequest);

        ArgumentCaptor<EvoplayFreeRoundPayoutRequest> payoutRequest = ArgumentCaptor.forClass(EvoplayFreeRoundPayoutRequest.class);
        verify(gameSessionService).verifyToken(TOKEN);
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("7857169864");
        verify(evoplayFreeRoundPayoutService).payout(payoutRequest.capture(), same(httpRequestLog));
        verify(winService, never()).win(any(), any(), any(), any());
        assertThat(payoutRequest.getValue().getId()).isEqualTo("8ut0i1v2itwps");
        assertThat(payoutRequest.getValue().getAmount()).isEqualByComparingTo("0");
        assertThat(payoutRequest.getValue().isPlayerUuidCampaignLookup()).isTrue();
    }

    @Test
    void callback_routesGeneralWinToNormalWinService() throws Exception {
        httpRequestLog.setRequestBody(signed(winBody(
                "rkmm1zil4ay8d",
                "7857337621",
                "0",
                generalDetails()
        )));
        ResponseVo normalWinResponse = new ResponseVo();
        when(winService.win(any(), same(gameSession), same(httpRequestLog), eq("trace-1"))).thenReturn(normalWinResponse);

        ResponseVo response = callbackAction.callback(servletRequest);

        verify(gameSessionService).verifyToken(TOKEN);
        verify(unsettledBetCachingService, never()).getTop1UnsettledBetWithRoundId("7857337621");
        verify(evoplayFreeRoundPayoutService, never()).payout(any(), any());
        verify(winService).win(any(), same(gameSession), same(httpRequestLog), eq("trace-1"));
        assertThat(response).isSameAs(normalWinResponse);
    }

    private String signed(String unsignedBody) {
        Map<String, Object> rawData = VendorService.convertBodyToDto(unsignedBody, LinkedHashMap.class);
        rawData.put("project", PROJECT_ID);
        rawData.put("version", Formats.CALLBACK_VERSION);
        VendorService.rearrangeMap(rawData);

        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(rawData, "");
        String signature = VendorService.md5(VendorService.buildSignature(formData, SIGNATURE_KEY));
        return unsignedBody + "&signature=" + signature;
    }

    private String winBody(String callbackId, String roundId, String amount, String details) {
        return "token=" + TOKEN
                + "&callback_id=" + callbackId
                + "&name=win"
                + "&data[round_id]=" + roundId
                + "&data[action_id]=" + roundId
                + "&data[final_action]=1"
                + "&data[amount]=" + amount
                + "&data[currency]=THB"
                + "&data[details]=" + details;
    }

    private String freeSpinDetails(String totalWin, String spinsLeft, String spinsDone) {
        return """
                {"symbols":[["11","9","10"],["11","11","0"],["0","11","3"],["1","1","10"],["3","3","3"]],"game":{"action":"extrabonusspin","handler":"extrabonusspin","version":"GCV_2.59.0,BV4.3.1.2,PV","game_id":5951,"absolute_name":"fullstate\\\\html5\\\\evoplay\\\\hotrionights","mobile":true},"denomination":1,"currency_rate":{"currency":"THB","rate":33.383726},"bet":0,"lines":10,"total_bet":0,"total_win":%s,"final_action":1,"round_mode":"bonus_spins","balance_after_pay":"217996.02","payout":"92.00","lent_pack_id":"5319","freespin":false,"single_spin":false,"freespins_left":0,"balance_before_pay":217996.02,"pay_for_action_this_round":0,"game_mode_code":2,"bonus_buy":false,"round":{"game_bet":0},"extrabonus_registration_id":"019fcb9fdfab7bbaaff6fe6da7943c7d","extrabonus_type":"bonus_spins","extrabonus":{"registry_id":"62714","spins":{"bet":10,"left":%s,"done":%s,"add":0},"last_action_in_chain_of_gifts":0},"first_action_in_round":1,"final_action_in_round":1,"total_bet_for_action_in_money":0,"total_win_for_action_in_money":%s}""".formatted(totalWin, spinsLeft, spinsDone, totalWin);
    }

    private String generalDetails() {
        return """
                {"symbols":[["0","0","0"],["3","3","3"],["2","2","2"],["7","7","8"],["0","0","0"]],"game":{"action":"spin","handler":"spin","version":"GCV_2.59.0,BV4.3.1.2,PV","game_id":5951,"absolute_name":"fullstate\\\\html5\\\\evoplay\\\\hotrionights","mobile":true},"denomination":1,"currency_rate":{"currency":"THB","rate":33.394497},"bet":10,"lines":10,"total_bet":10,"total_win":0,"final_action":1,"round_mode":"general","balance_after_pay":"218011.02","payout":"92.00","lent_pack_id":"5319","freespin":false,"single_spin":true,"freespins_left":0,"balance_before_pay":218021.02,"pay_for_action_this_round":10,"game_mode_code":0,"bonus_buy":false,"round":{"game_bet":10},"first_action_in_round":1,"final_action_in_round":1,"total_bet_for_action_in_money":10,"total_win_for_action_in_money":0}""";
    }
}
