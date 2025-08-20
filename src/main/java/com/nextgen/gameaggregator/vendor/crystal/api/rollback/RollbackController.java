package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RollbackController {
    private final RollbackRequestMapper requestMapper;
    private final RollbackResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public RollbackController(RollbackRequestMapper requestMapper, RollbackResponseMapper responseMapper, WalletRollbackServiceWrapper walletService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletService = walletService;
    }


    @PostMapping(path = EndPoints.REFUND)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public RollbackResponse doRollback(@Valid @RequestBody RollbackRequest request) {
        BetRollbackContext context = requestMapper.toInternal(request);
        PlayerBalanceData balanceData = walletService.process(context);
        return responseMapper.toVendor(context, balanceData);
    }
}