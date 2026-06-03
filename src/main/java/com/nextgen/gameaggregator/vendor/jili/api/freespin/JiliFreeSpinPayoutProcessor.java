package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class JiliFreeSpinPayoutProcessor {

    private final JiliFreeSpinPayoutHandler jiliFreeSpinPayoutHandler;

    public JiliFreeSpinPayoutProcessor(JiliFreeSpinPayoutHandler jiliFreeSpinPayoutHandler) {
        this.jiliFreeSpinPayoutHandler = jiliFreeSpinPayoutHandler;
    }

    public BetVo process(BetDto betDto,
                         String vendorPlayerUsername,
                         String vendorCurrencyCode,
                         String token) throws InvalidRequestException {
        // N3: free spin payout cannot be negative
        if (betDto.getWinloseAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("winloseAmount cannot be negative for free spin payout");
        }

        JiliFreeSpinPayoutRequest request = JiliFreeSpinPayoutRequest.builder()
                .vendorPlayerUsername(vendorPlayerUsername)
                .vendorCurrencyCode(vendorCurrencyCode)
                .token(token)
                .reqId(betDto.getReqId())
                .round(String.valueOf(betDto.getRound()))
                .winloseAmount(betDto.getWinloseAmount())
                .wagersTime(betDto.getWagersTime())
                .freeSpinData(betDto.getFreeSpinData())
                .build();

        return jiliFreeSpinPayoutHandler.process(request);
    }
}
