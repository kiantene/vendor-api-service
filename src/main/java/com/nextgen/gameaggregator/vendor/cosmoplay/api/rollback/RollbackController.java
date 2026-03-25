package com.nextgen.gameaggregator.vendor.cosmoplay.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {

    protected RollbackController(
            BetRollbackContextMapper<RollbackRequest> requestMapper,
            BetRollbackVendorResponseMapper<RollbackResponse> responseMapper,
            WalletRollbackServiceWrapper walletService
    ) {
        super(requestMapper, responseMapper, walletService);
    }

    @Override
    protected void configure(BetRollbackConfig config, RollbackRequest request) {
        config
            .rollbackType(RollbackType.BY_BET)
            .returnSuccessOnDuplicate(true);
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = CosmoPlayVendorConfig.CLASS_NAME)
    public ResponseEntity<RollbackResponse> handle(
            @Valid
            @RequestBody
            RollbackRequest request
    ) {
        return ResponseEntity.ok(processRequest(request));
    }
}
