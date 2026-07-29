package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.SettleByRoundScenario;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SettleByRoundOperatorBetResultRequestMapper extends AbstractOperatorBetResultRequestMapper<SettleByRoundScenario> {

    @Override
    protected void mapScenario(OperatorBetResultRequest request, OperatorApiContext<BetResultContext> apiContext, SettleByRoundScenario scenario) {

        ResultType resultType = scenario.getResultType();

        request.setBetId(scenario.getBetTxn().getGaBetId());
        request.setTransactionId(UuidUtil.newUuidV7String());
        request.setBetTime(scenario.getBetTxn().getBetTime());
        request.setResultType(resultType);

        /**
         * For SettleByRound, an Async Process will be sending a seperate ResultType=END request to Operator for the complete Round Details
         */
        request.setIsEndRound(0);

        TxnAmounts txnAmounts = TxnAmounts.ofCapped(scenario.getBetTxn(), apiContext.getTransaction(), apiContext.getContext().getFromVendorRate());

        request.setBetAmount(getBetAmount(resultType, txnAmounts.getBet()));
        request.setWinAmount(txnAmounts.getWin());
        request.setJackpotAmount(txnAmounts.getJackpot());

        /**
         * Settle By Round to set Turnover and WinLoss to 0 for all Intermediate Calls.
         * There will be a separate service ga-game-round-ended-service that will send a Result with the FINAL round information
         */
        request.setEffectiveTurnover(BigDecimal.ZERO);
        request.setWinLoss(BigDecimal.ZERO);

    }
}
