package com.nextgen.gameaggregator.vendor.gpkv2.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackService extends AbstractBetRollbackController<RollbackRequest, CommonVo> {
    public RollbackService(RollbackRequestMapper requestMapper,
            RollbackResponseMapper responseMapper,
            WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<CommonVo> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {

        config.rollbackType(RollbackType.BY_BET)
                .returnSuccessOnDuplicate(true);
    }
}
