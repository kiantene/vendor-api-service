package com.nextgen.gameaggregator.vendor.evolutionv2.response;

import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceProcessor;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.api.promo.PromoPayoutRequestDto;
import com.nextgen.gameaggregator.vendor.evolutionv2.api.promo.PromoPayoutRequestMapper;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Evolution v2 promo-payout integration.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionResponsePostProcessorTest {

    @Mock private PromoPayoutRequestMapper requestMapper;
    @Mock private BalanceProcessor balanceProcessor;

    private EvolutionResponsePostProcessor processor;
    private PromoPayoutRequestDto request;

    @BeforeEach
    void setUp() {
        processor = new EvolutionResponsePostProcessor(requestMapper, balanceProcessor);
        request = new PromoPayoutRequestDto();
        request.setUuid("request-123");

        LogContext logContext = new LogContext();
        logContext.setTraceId("trace-123");
        LogContextHolder.set(logContext);
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @Test
    void postProcessErrorResponse_echoesRequestUuid() {
        ResponseVo response = new ResponseVo();
        response.setResponseCode(ResponseCode.INVALID_PARAMETER);

        VendorErrorResponse result = processor.postProcessErrorResponse(
                new VendorErrorResponse(response),
                VendorExceptionContext.of(new Object[]{request}, Map.of())
        );

        assertThat(result.getBody()).isSameAs(response);
        assertThat(response.getUuid()).isEqualTo("request-123");
        assertThat(processor.getVendorClassName()).isEqualTo(EndPoints.CLASS_NAME);
    }

    @Test
    void postProcessDuplicateResponse_addsCurrentBalance() {
        PromoPayoutContext context = PromoPayoutContext.builder().build();
        PlayerBalanceData balance = new PlayerBalanceData(
                "player123",
                "USD",
                new BigDecimal("125.50"),
                1754373936436L
        );
        ResponseVo response = new ResponseVo();
        when(requestMapper.toInternal(request)).thenReturn(context);
        when(balanceProcessor.process("trace-123", context)).thenReturn(balance);

        processor.postProcessErrorResponse(
                new VendorErrorResponse(response),
                VendorExceptionContext.of(new Object[]{request}, Map.of())
        );

        assertThat(response.getResponseCode()).isEqualTo(ResponseCode.OK);
        assertThat(response.getBalance()).isEqualByComparingTo("125.50");
        assertThat(response.getUuid()).isEqualTo("request-123");
    }
}
