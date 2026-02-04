package com.nextgen.gameaggregator.core.engine.operator.wallet.result;

import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;
import com.nextgen.gameaggregator.core.engine.operator.wallet.OperatorRequestMapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
public abstract class AbstractOperatorBetResultRequestMapper<S extends OperatorScenario> implements OperatorRequestMapper<BetResultContext, OperatorBetResultRequest, S> {

    @Override
    public OperatorBetResultRequest toOperatorRequest(OperatorApiContext<BetResultContext> operatorApiContext, S scenario) {
        BetResultContext context = operatorApiContext.getContext();
        GameRound round = operatorApiContext.getRound();

        OperatorBetResultRequest request = new OperatorBetResultRequest();

        request.setTraceId(operatorApiContext.getContext().getTraceId());
        request.setUsername(context.getAgentPlayerUsername());
        request.setRoundId(context.getRoundId());
        request.setIsFreespin(context.getIsFreeSpin());
        request.setCurrency(round.getAgentMeta().getCurrency());
        request.setToken(round.getAgentMeta().getSession());
        request.setGameCode(context.getGameCode());
        request.setSettledTime(context.getResultTime());
        request.setIsEndRound(context.getRoundEnded() ? 1 : 0);
        request.setExternalTransactionId(context.getIdempotencyKey());

        mapScenario(request, operatorApiContext, scenario);

        return request;
    }

    protected abstract void mapScenario(OperatorBetResultRequest request, OperatorApiContext<BetResultContext> operatorApiContext, S scenario);

    protected BigDecimal getBetAmount(ResultType resultType,  BigDecimal betAmount) {
        switch (resultType) {
            case WIN, LOSE -> { return BigDecimal.ZERO; }
            case BET_WIN, BET_LOSE, END -> { return betAmount; }
        }
        return BigDecimal.ZERO;
    }
}