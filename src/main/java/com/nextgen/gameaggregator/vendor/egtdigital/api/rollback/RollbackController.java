package com.nextgen.gameaggregator.vendor.egtdigital.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceProcessor;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, RollbackResponse> {
    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService,
                              BalanceProcessor balanceProcessor) {
        super(requestMapper, responseMapper, walletService);
        this.balanceProcessor = balanceProcessor;
    }

    private final BalanceProcessor balanceProcessor;
    private static final Long ZERO_NUM = 0L;

    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME + "Rollback")
    public ResponseEntity<RollbackResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(processRequest(request, null, this::updateBalanceResponse));
    }

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(RollbackType.BY_ROUND).returnSuccessOnDuplicate(true);
    }

    private void updateBalanceResponse(BetRollbackContext context, RollbackResponse response) {
        if (response.getBalance() == null || response.getBalance().equals(ZERO_NUM)) {
            PlayerBalanceData playerBalanceData = balanceProcessor.process(context.getTraceId(), context);
            response.setBalance(Amount.vendor(playerBalanceData.getBalance()));
        }
    }
}
