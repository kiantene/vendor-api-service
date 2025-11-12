package com.nextgen.gameaggregator.core.engine.promo;

import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.promo.payout.*;
import com.nextgen.gameaggregator.enums.PromoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class PromoPayoutServiceTest {

    @InjectMocks
    private PromoPayoutServiceImpl promoPayoutService;

    @Mock
    private PromoPayoutValidator validator;
    @Mock
    private PromoPayoutContextEnricher enricher;
    @Mock
    private PromoPayoutProcessor processor;
    @Mock
    private ClientRequestService clientRequestService;
    @Mock
    private PromoPayoutMapper mapper;

    private PromoPayoutContext context;

    @BeforeEach
    void setUp() {

        // Create test data
        context = PromoPayoutContext.builder()
                .idempotencyKey("1952611929575260160-1952611929575260160-403-0")
                .vendorPlayerUsername("1e8yrfpf5qr")
                .vendorCurrency("THB")
                // promo payout history
                .vendorTransactionId("1952611929575260160-1952611929575260160-403-0")
                .vendorPayoutAmount(BigDecimal.TEN)
                .vendorTransactionTime(1754373936436L)
                .promoType(PromoType.FREE_ROUND)
                .build();
    }

//    @Test
//    void testProcess() {
//        // Arrange
//        doNothing().when(validator).validateOrThrow(context);
//
//        // Stub enricher.enrich to modify context
//        doAnswer(invocation -> {
//            PromoPayoutContext ctx = invocation.getArgument(0);
//            ctx.setAgentPlayerId(111L);
//            ctx.setVendorPlayerId(110L);
//            ctx.setVendorId(2);
//            ctx.setVendorLineId(2);
//            ctx.setAgentPlayerUsername("load123");
//            ctx.setAgentId(10);
//            ctx.setMasterAgentId(0);
//            ctx.setHouseId(0);
//            ctx.setCurrencyId(7);
//            ctx.setCurrencyCode("BRL");
//            ctx.setVendorCode("PGS");
//            ctx.setVendorGameId(1000);
//            ctx.setGameCode("GameCode");
//            ctx.setGameName("Game Name");
//            ctx.setGameCategoryId(1);
//            ctx.setGameCategoryCode("SLOT");
//            return null;
//        }).when(enricher).enrich(context);
//
//        doNothing().when(processor).process(context);
//        PromoPayoutRequest promoPayoutRequest = PromoPayoutRequest.builder()
//                .traceId(context.getTraceId())
//                .username(context.getAgentPlayerUsername())
//                .transactionId(context.getTransactionId())
//                .currency(context.getCurrencyCode())
//                .amount(context.getPayoutAmount())
//                .type(context.getPromoType().code)
//                .timestamp(context.getVendorTransactionTime())
//                .build();
//        when(mapper.toPromoPayoutRequest(context)).thenReturn(promoPayoutRequest);
//        doNothing().when(clientRequestService).createClientApiRequest(10, EndPoints.PROMO_PAYOUT, promoPayoutRequest);
//
//        when(clientRequestService.shouldMockResponse("load123")).thenReturn(Boolean.TRUE);
//        PlayerBalanceData playerBalanceData = new PlayerBalanceData(
//                context.getAgentPlayerUsername(),
//                context.getCurrencyCode(),
//                BigDecimal.ONE,
//                System.currentTimeMillis()
//        );
//
//        ClientBalanceResponse response = new ClientBalanceResponse();
//        response.setStatus(ResponseCodes.Status.SC_OK.toString());
//        response.setTraceId(context.getTraceId());
//        response.setMessage(ResponseCodes.Status.SC_OK.description + " : Skip Stub");
//        response.setData(playerBalanceData);
//        when(clientRequestService.mockClientResponse(context.getTraceId(), "BRL", "load123"))
//                .thenReturn(response);
//
//        // Act
//        PlayerBalanceData data = promoPayoutService.process(context);
//
//        // Assert
//        assertNotNull(data);
//    }
}
