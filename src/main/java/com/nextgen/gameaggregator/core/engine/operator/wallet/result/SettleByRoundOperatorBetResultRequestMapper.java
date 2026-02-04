package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.SettleByRoundScenario;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.springframework.stereotype.Component;

@Component
public class SettleByRoundOperatorBetResultRequestMapper extends AbstractOperatorBetResultRequestMapper<SettleByRoundScenario> {

    @Override
    protected void mapScenario(OperatorBetResultRequest request, OperatorApiContext<BetResultContext> apiContext, SettleByRoundScenario scenario) {

        BetResultContext context = apiContext.getContext();
        ResultType resultType = scenario.getResultType(apiContext.getRound());

        request.setBetId(scenario.getBetTxn().getGaBetId());
        request.setTransactionId(UuidUtil.newUuidV7String());
        request.setBetTime(scenario.getBetTxn().getBetTime());
        request.setResultType(resultType);

        TxnAmounts txnAmounts = TxnAmounts.of(apiContext.getRound(), apiContext.getTransaction(), context.getFromVendorRate());

        request.setBetAmount(getBetAmount(resultType, txnAmounts.getBet()));
        request.setWinAmount(txnAmounts.getWin());
        request.setEffectiveTurnover(txnAmounts.getTurnover());
        request.setJackpotAmount(txnAmounts.getJackpot());
        request.setWinLoss(txnAmounts.getWinLoss());

    }
}
