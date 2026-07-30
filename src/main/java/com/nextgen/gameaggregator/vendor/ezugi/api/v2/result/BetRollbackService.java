package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;


@Service
public class BetRollbackService extends AbstractBetRollbackController<BetResultRequest, BetResultResponse> {

    protected BetRollbackService(BetRollbackRequestMapper requestMapper,
                                 BetRollbackResponseMapper responseMapper,
                                 WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public BetResultResponse rollback(@Valid @RequestBody BetResultRequest request) {
        return this.processRequest(
                request,
                (context, resp) -> enrichResponse(resp, request)
        );
    }

    @Override
    public void configure(BetRollbackConfig config, BetResultRequest request) {
        config.rollbackType(RollbackType.BY_BET);
    }

    private void enrichResponse(BetResultResponse response, BetResultRequest request) {
        response.setOperatorId(request.getOperatorId());
        response.setRoundId(request.getRoundId());
    }
}
