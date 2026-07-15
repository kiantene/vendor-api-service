package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabaneroBonusPayoutHandlerTest {

    @Mock private HabaneroBonusPayoutRequestMapper requestMapper;
    @Mock private HabaneroBonusPayoutResponseMapper responseMapper;
    @Mock private PromoPayoutService promoPayoutService;

    @InjectMocks
    private HabaneroBonusPayoutHandler handler;

    @Test
    void process_delegatesRequestThroughMapperAndService() {
        HabaneroBonusPayoutRequest request = HabaneroBonusPayoutRequest.builder()
                .fundTransferRequest(new FundTransferRequestDto())
                .fundInfo(new FundInfoDto())
                .bonusDetails(new BonusDetailDto())
                .vendorGameCode("game-001")
                .build();

        PromoPayoutContext context = PromoPayoutContext.builder()
                .vendorCurrency("USD")
                .vendorPayoutAmount(BigDecimal.TEN)
                .promoType(PromoType.FREE_ROUND)
                .build();

        PlayerBalanceData balanceData = new PlayerBalanceData("player01", "USD", BigDecimal.TEN, System.currentTimeMillis());

        TransferVo expectedResponse = new TransferVo();

        when(requestMapper.toInternal(request)).thenReturn(context);
        when(promoPayoutService.initialise(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.configure(any())).thenReturn(promoPayoutService);
        when(promoPayoutService.process(any())).thenReturn(balanceData);
        when(responseMapper.toVendor(any(), any())).thenReturn(expectedResponse);

        TransferVo result = handler.process(request);

        assertThat(result).isSameAs(expectedResponse);
    }
}
