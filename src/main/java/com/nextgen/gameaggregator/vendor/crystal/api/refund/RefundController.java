package com.nextgen.gameaggregator.vendor.crystal.api.refund;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RefundController {
    private final RefundRequestMapper requestMapper;
    private final RefundResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public RefundController(RefundRequestMapper requestMapper, RefundResponseMapper responseMapper, WalletRollbackServiceWrapper walletService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletService = walletService;
    }


    @PostMapping(path = EndPoints.REFUND)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public RefundResponse doRollback(RefundRequest request) {
        BetRollbackContext context = requestMapper.toBetRollbackContext(request);
        PlayerBalanceData balanceData = walletService.process(context);
        return responseMapper.toVendor(context, balanceData);
    }
}