package com.nextgen.gameaggregator.vendor.lucky365.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.lucky365.constant.GameStatus;
import com.nextgen.gameaggregator.vendor.lucky365.constant.Mode;
import com.nextgen.gameaggregator.vendor.lucky365.util.TimeStamp;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {

        BigDecimal betAmount = BigDecimal.ZERO;
        BigDecimal winAmount = BigDecimal.ZERO;
        BigDecimal jackpotAmount = BigDecimal.ZERO;
        int isFreeSpin = 0;

        GameStatus status = GameStatus.fromCode(request.getGameStatus());
        Mode mode = Mode.fromCode(request.getMode());

        if (status == GameStatus.FREE) {
            winAmount = request.getTotalWin();
            isFreeSpin = 1;
        } else if (status.isJackpot()) {
            jackpotAmount = request.getTotalWin();
        } else {
            betAmount = (mode == Mode.BET_AND_RESULT && request.getBet() != null)
                    ? request.getBet().getTotalBet()
                    : BigDecimal.ZERO;
            winAmount = request.getTotalWin();
        }

        return BetResultContext.builder()
                .idempotencyKey(request.getId())
                .vendorBetId(request.getOrderCode())
                .vendorPlayerUsername(request.getLoginId().toLowerCase(Locale.ROOT))
                .vendorGameCode(request.getGameCode())
                .roundId(request.getOrderCode())
                .betAmount(betAmount)
                .winAmount(winAmount)
                .vendorSettleTime(TimeStamp.convertTimeStamp(request.getActionDate()))
                .vendorBetTime(TimeStamp.convertTimeStamp(request.getActionDate()))
                .isFreeSpin(isFreeSpin)
                .jackpotAmount(jackpotAmount)
                .build();
    }
}