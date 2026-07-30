package com.nextgen.gameaggregator.core.engine.operator.wallet.bet;

import com.nextgen.gameaggregator.core.engine.operator.BetScenario;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.wallet.OperatorRequestMapper;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperatorBetRequestMapper implements OperatorRequestMapper<BetContext, OperatorBetRequest, BetScenario> {

    @Override
    public OperatorBetRequest toOperatorRequest(OperatorApiContext<BetContext> operatorApiContext, BetScenario scenario) {
        BetContext context = operatorApiContext.getContext();
        GameTransaction txn = operatorApiContext.getTransaction();
        GameRound round = operatorApiContext.getRound();

        OperatorBetRequest dto = new OperatorBetRequest();

        dto.setTraceId(context.getTraceId());
        dto.setBetId(txn.getGaBetId());
        dto.setTransactionId(txn.getGaBetId());
        dto.setUsername(context.getAgentPlayerUsername());
        dto.setCurrency(round.getAgentMeta().getCurrency());
        dto.setToken(round.getAgentMeta().getSession());
        dto.setExternalTransactionId(context.getIdempotencyKey());
        dto.setGameCode(context.getGameCode());
        dto.setRoundId(context.getRoundId());
        dto.setTimestamp(context.getTimestamp());

        TxnAmount txnAmount = TxnAmount.of(context.getBetAmount(), context.getFromVendorRate());
        dto.setAmount(txnAmount.amount());

        return dto;
    }
}
