package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
class BetResultDataMapper {
    public BetResultData toBetResultData(BetResultContext context) {
        return new BetResultData() {
            @Override
            public String getExternalTransactionId() {
                return context.getIdempotencyKey();
            }

            @Override
            public String getVendorBetId() {
                return context.getVendorBetId();
            }

            @Override
            public String getRoundId() {
                return context.getRoundId();
            }

            @Override
            public String getGameId() {
                return context.getGameCode();
            }

            @Override
            public BigDecimal getBetAmount() {
                return context.getBetAmount();
            }

            @Override
            public BigDecimal getWinAmount() {
                return context.getWinAmount();
            }

            @Override
            public BigDecimal getWinLoss() {
                return context.getWinloss();
            }

            @Override
            public BigDecimal getEffectiveTurnover() {
                return context.getEffectiveTurnover();
            }

            @Override
            public Long getVendorBetTime() {
                return context.getVendorBetTime();
            }

            @Override
            public Long getResultTime() {
                return context.getResultTime();
            }

            @Override
            public Long getVendorSettleTime() {
                return context.getVendorSettleTime();
            }

            @Override
            public BigDecimal getJackpotAmount() {
                return context.getJackpotAmount();
            }

            @Override
            public Integer getIsFreespin() {
                return context.getIsFreeSpin();
            }

            @Override
            public BetStatus getBetStatus() {
                return BetStatus.SETTLED;
            }
        };
    }
}
