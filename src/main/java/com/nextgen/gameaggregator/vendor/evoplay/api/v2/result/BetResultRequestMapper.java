package com.nextgen.gameaggregator.vendor.evoplay.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<CallbackDto> {
    private static final String ROUND_END = "1";
    @Override
    public BetResultContext toInternal(CallbackDto request) {

        boolean isRoundEnded = ROUND_END.equals(request.getData().getFinal_action());
        int isFreeSpin = (request.getData().getDetailsDto().getTotal_bet().compareTo(BigDecimal.ZERO) == 0) ? 1 : 0;
        return BetResultContext.builder()
                .idempotencyKey(request.getCallback_id())
                .token(request.getToken())
                .vendorGameCode(request.getData().getDetailsDto().getGame().getGame_id())
                .roundId(request.getData().getRound_id())
                .vendorBetId(request.getData().getAction_id() != null ?
                        request.getData().getAction_id() :
                        request.getData().getRound_id())
                .winAmount(new BigDecimal(request.getData().getAmount()))
                .vendorPlayerUsername(request.getUsername())
                .isFreeSpin(isFreeSpin)
                .roundEnded(isRoundEnded)
                .build();
    }

}
