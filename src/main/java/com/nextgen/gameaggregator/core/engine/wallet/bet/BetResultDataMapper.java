package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class BetResultDataMapper {
    public BetResultData toBetResultData(BetContext context, String gaBetId) {
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
                return context.getVendorGameCode();
            }

            @Override
            public BigDecimal getBetAmount() {
                return context.getBetAmount();
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
                return context.getBetAmount();
            }

            @Override
            public Long getVendorBetTime() {
                return context.getTimestamp();
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

            @Override
            public boolean isNewFramework() { return true; }

            @Override
            public String getGaBetId() { return gaBetId; }
        };
    }
}
