package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.gameaggregator.core.engine.operator.BetAndResultScenario;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.springframework.stereotype.Component;

@Component
public class BetAndResultOperatorBetResultRequestMapper extends AbstractOperatorBetResultRequestMapper<BetAndResultScenario> {

    @Override
    protected void mapScenario(OperatorBetResultRequest request, OperatorApiContext<BetResultContext> operatorApiContext, BetAndResultScenario scenario) {

        BetResultContext context = operatorApiContext.getContext();

        request.setBetId(operatorApiContext.getTransaction().getGaBetId());
        request.setTransactionId(request.getBetId());
        request.setBetTime(context.getResultTime());
        request.setResultType(scenario.getResultType());

        TxnAmounts txnAmounts = TxnAmounts.of(operatorApiContext.getTransaction(), context.getFromVendorRate());

        request.setBetAmount(txnAmounts.getBet());
        request.setWinAmount(txnAmounts.getWin());
        request.setEffectiveTurnover(txnAmounts.getTurnover());
        request.setJackpotAmount(txnAmounts.getJackpot());
        request.setWinLoss(txnAmounts.getWinLoss());

    }
}
