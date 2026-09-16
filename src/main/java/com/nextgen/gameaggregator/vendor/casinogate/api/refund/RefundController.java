package com.nextgen.gameaggregator.vendor.casinogate.api.refund;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class RefundController extends AbstractBetRollbackController<RefundRequest, CommonResponse> {
    public RefundController(RefundRequestMapper requestMapper,
                            RefundResponseMapper responseMapper,
                            WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @PostMapping(path = Endpoints.REFUND)
    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<CommonResponse> refund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    protected void configure(BetRollbackConfig config, RefundRequest request) {
        config.rollbackType(RollbackType.BY_BET)
                .allowRollbackForSettledBet(true)
                .returnSuccessOnDuplicate(false)
                .validateAmountWithBet(true);
    }
}
