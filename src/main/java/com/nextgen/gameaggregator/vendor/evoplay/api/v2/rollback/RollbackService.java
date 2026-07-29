package com.nextgen.gameaggregator.vendor.evoplay.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.*;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RollbackService extends AbstractBetRollbackController<CallbackDto, ResponseVo> {

    protected RollbackService(BetRollbackContextMapper<CallbackDto> requestMapper,
                              BetRollbackVendorResponseMapper<ResponseVo> responseMapper,
                              WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<ResponseVo> rollback(CallbackDto request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetRollbackConfig config, CallbackDto request) {

        config.rollbackType(RollbackType.BY_BET)
                .returnSuccessOnDuplicate(true);
    }
}