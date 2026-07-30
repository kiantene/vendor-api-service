package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.SettleByBetScenario;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.service.data.model.TxnAmounts;
import org.springframework.stereotype.Component;

@Component
public class SettleByBetOperatorBetRequestMapper extends AbstractOperatorBetResultRequestMapper<SettleByBetScenario> {

    @Override
    protected void mapScenario(OperatorBetResultRequest request, OperatorApiContext<BetResultContext> operatorApiContext, SettleByBetScenario scenario) {

        request.setBetId(scenario.getBetTxn().getGaBetId());
        request.setTransactionId(UuidUtil.newUuidV7String());
        request.setBetTime(scenario.getBetTxn().getBetTime());
        request.setResultType(scenario.getResultType());

        TxnAmounts txnAmounts = TxnAmounts.ofCapped(scenario.getBetTxn(), operatorApiContext.getTransaction(), operatorApiContext.getContext().getFromVendorRate());

        request.setBetAmount(getBetAmount(scenario.getResultType(), txnAmounts.getBet()));
        request.setWinAmount(txnAmounts.getWin());
        request.setEffectiveTurnover(txnAmounts.getTurnover());
        request.setJackpotAmount(txnAmounts.getJackpot());
        request.setWinLoss(txnAmounts.getWinLoss());

    }
}

/**
 * From Old Wallet Service. Still needed?
 *
 *         // TODO: IS THIS STILL NEEDED FOR NEW FRAMEWORK
 * //        Integer agentId = Optional.ofNullable(betInformation.getAgentId()).orElse(gameSession.getAgentId());
 * //        Integer vendorId = Optional.ofNullable(betInformation.getVendorId()).orElse(gameSession.getVendorId());
 * //
 * //        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(agentId);
 * //
 * //        if (betInformation.getIsEndRound() != null && agentApiVersion == 2 && this.skipVendorList.contains(vendorId)) {
 * //            //if isEndRound configure not empty from DTO, and agentApiVersion is 2, and is PGSOFT and SPADEGAMING then set the isEndRound value
 * //            walletBetResultDto.setIsEndRound(betInformation.getIsEndRound());
 * //        }
 */