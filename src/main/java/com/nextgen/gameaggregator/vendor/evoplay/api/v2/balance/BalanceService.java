package com.nextgen.gameaggregator.vendor.evoplay.api.v2.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalanceService extends AbstractBalanceController<CallbackDto, ResponseVo> {
    protected BalanceService(BalanceRequestMapper requestMapper,
                             BalanceResponseMapper responseMapper,
                             WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    public ResponseEntity<ResponseVo> getBalance(CallbackDto request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
