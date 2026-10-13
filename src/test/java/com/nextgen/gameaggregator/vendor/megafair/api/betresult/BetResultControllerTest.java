package com.nextgen.gameaggregator.vendor.megafair.api.betresult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.vendor.megafair.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.megafair.api.bet.BetRequestMapper;
import com.nextgen.gameaggregator.vendor.megafair.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.megafair.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.megafair.api.result.BetResultRequestMapper;
import com.nextgen.gameaggregator.vendor.megafair.api.result.BetResultService;
import com.nextgen.gameaggregator.vendor.megafair.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.megafair.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResultControllerTest {

    @Mock
    private BetService betService;

    @Mock
    private BetResultService betResultService;

    @Mock
    private VendorUtil vendorUtil;

    @InjectMocks
    private BetResultController controller;

    private CommonResponse dummyResponse;

    @BeforeEach
    void setUp() {
        dummyResponse = CommonResponse.builder()
                .response(CommonResponse.ResponseData.builder()
                        .result("OK")
                        .extSystemTransactionId("ext_tx_100")
                        .build())
                .build();
    }

    @Test
    @DisplayName("bet(): Routes to BetService when win field is null")
    void bet_RoutesToBetService() {
        CommonRequest request = CommonRequest.builder()
                .userId("player1")
                .bet("100|tx_001")
                .win(null)
                .roundId("r1")
                .gameId("g1")
                .gameSessionId("s1")
                .isRoundFinished("0")
                .hash("hash123")
                .build();

        when(vendorUtil.convertDto(any(CommonRequest.class), eq(BetRequest.class)))
                .thenReturn(BetRequest.builder().build());
        when(betService.bet(any(BetRequest.class))).thenReturn(dummyResponse);

        ResponseEntity<CommonResponse> responseEntity = controller.bet(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();

        verify(vendorUtil).parseFormattedFields(request);
        verify(betService, times(1)).bet(any(BetRequest.class));
        verify(betResultService, never()).result(any());
    }

    @Test
    @DisplayName("bet(): Routes to BetResultService when win field is present")
    void bet_RoutesToBetResultService() {
        CommonRequest request = CommonRequest.builder()
                .userId("player1")
                .bet("100|tx_001")
                .win("500|tx_002")
                .roundId("r1")
                .gameId("g1")
                .gameSessionId("s1")
                .isRoundFinished("1")
                .hash("hash123")
                .build();

        when(vendorUtil.convertDto(any(CommonRequest.class), eq(BetResultRequest.class)))
                .thenReturn(BetResultRequest.builder().build());
        when(betResultService.result(any(BetResultRequest.class))).thenReturn(dummyResponse);

        ResponseEntity<CommonResponse> responseEntity = controller.bet(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();

        verify(vendorUtil).parseFormattedFields(request);
        verify(betResultService, times(1)).result(any(BetResultRequest.class));
        verify(betService, never()).bet(any());
    }

    @Test
    @DisplayName("enrichResponse: Enriches XML response block with request mirror data")
    void bet_EnrichesResponseCorrectly() {
        CommonRequest request = CommonRequest.builder()
                .userId("user_abc")
                .bet("50|tx_bet")
                .win("0|tx_win")
                .roundId("round_99")
                .gameId("game_77")
                .gameSessionId("sess_11")
                .isRoundFinished("1")
                .kind("freespin")
                .hash("hash_secret")
                .build();

        when(vendorUtil.convertDto(any(CommonRequest.class), eq(BetResultRequest.class)))
                .thenReturn(BetResultRequest.builder().build());
        when(betResultService.result(any(BetResultRequest.class))).thenReturn(dummyResponse);

        ResponseEntity<CommonResponse> responseEntity = controller.bet(request);

        CommonResponse body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRequest()).isNotNull();
        assertThat(body.getRequest().getUserId()).isEqualTo("user_abc");
        assertThat(body.getRequest().getBet()).isEqualTo("50|tx_bet");
        assertThat(body.getRequest().getWin()).isEqualTo("0|tx_win");
        assertThat(body.getRequest().getIsRoundFinished()).isEqualTo("1");
    }

    @Test
    @DisplayName("parseAndMap: Real parseFormattedFields and BetRequestMapper correctly preserve raw bet amount '100|tx'")
    void parseAndMap_PreservesRawBetAmount() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        WalletBalanceService mockWalletBalanceService = mock(WalletBalanceService.class);
        VendorUtil realVendorUtil = new VendorUtil(validator, new ObjectMapper(), mockWalletBalanceService);

        BetRequestMapper mapper = new BetRequestMapper();

        CommonRequest request = CommonRequest.builder()
                .userId("player1")
                .bet("100|tx_001")
                .win(null)
                .roundId("r1")
                .gameId("g1")
                .gameSessionId("s1")
                .isRoundFinished("0")
                .hash("hash123")
                .build();

        realVendorUtil.parseFormattedFields(request);

        assertThat(request.getBetAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(request.getBetTransactionId()).isEqualTo("tx_001");

        BetRequest betRequest = realVendorUtil.convertDto(request, BetRequest.class);
        assertThat(betRequest).isNotNull();

        BetContext betContext = mapper.toInternal(betRequest);

        assertThat(betContext).isNotNull();
        assertThat(betContext.getBetAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(betContext.getIdempotencyKey()).isEqualTo("tx_001");
    }

    @Test
    @DisplayName("parseAndMap: Real parseFormattedFields and BetResultRequestMapper correctly preserve raw win amount '500|tx'")
    void parseAndMap_PreservesRawWinAmount_ForWinPath() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        WalletBalanceService mockWalletBalanceService = mock(WalletBalanceService.class);
        VendorUtil realVendorUtil = new VendorUtil(validator, new ObjectMapper(), mockWalletBalanceService);

        BetResultRequestMapper mapper = new BetResultRequestMapper();

        CommonRequest request = CommonRequest.builder()
                .userId("player1")
                .bet("100|tx_001")
                .win("500|tx_002")
                .roundId("r1")
                .gameId("g1")
                .gameSessionId("s1")
                .isRoundFinished("1")
                .hash("hash123")
                .build();

        realVendorUtil.parseFormattedFields(request);

        assertThat(request.getWinAmount()).isEqualTo(new BigDecimal("500"));
        assertThat(request.getWinTransactionId()).isEqualTo("tx_002");

        BetResultRequest resultRequest = realVendorUtil.convertDto(request, BetResultRequest.class);
        assertThat(resultRequest).isNotNull();

        BetResultContext resultContext = mapper.toInternal(resultRequest);

        assertThat(resultContext).isNotNull();
        assertThat(resultContext.getWinAmount()).isEqualTo(new BigDecimal("500"));
        assertThat(resultContext.getIdempotencyKey()).isEqualTo("tx_002");
        assertThat(resultContext.isRoundEnded()).isTrue();
    }
}
