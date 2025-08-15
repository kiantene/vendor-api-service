package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class BetResultDataMapper {
    public BetResultData toBetResultData(BetContext betContext) {
        return new BetResultData() {
            @Override
            public String getExternalTransactionId() {
                return betContext.getIdempotencyKey();
            }

            @Override
            public String getVendorBetId() {
                return betContext.getVendorBetId();
            }

            @Override
            public String getRoundId() {
                return betContext.getRoundId();
            }

            @Override
            public String getGameId() {
                return betContext.getGameCode();
            }

            @Override
            public BigDecimal getBetAmount() {
                return betContext.getBetAmount();
            }

            @Override
            public BigDecimal getWinAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getWinLoss() {
                return BigDecimal.ZERO;
            }

            @Override
            public BigDecimal getEffectiveTurnover() {
                return betContext.getBetAmount();
            }

            @Override
            public Long getVendorBetTime() {
                return betContext.getTimestamp();
            }

            @Override
            public Long getResultTime() {
                return null;
            }

            @Override
            public Long getVendorSettleTime() {
                return null;
            }

            @Override
            public BigDecimal getJackpotAmount() {
                return BigDecimal.ZERO;
            }

            @Override
            public Integer getIsFreespin() {
                return 0;
            }

            @Override
            public BetStatus getBetStatus() {
                return BetStatus.UNSETTLED;
            }
        };
    }
}
