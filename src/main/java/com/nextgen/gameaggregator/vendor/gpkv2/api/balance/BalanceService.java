package com.nextgen.gameaggregator.vendor.gpkv2.api.balance;

import com.nextgen.gameaggregator.core.engine.wallet.balance.AbstractBalanceController;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalanceService extends AbstractBalanceController<CommonDto, CommonVo> {
    protected BalanceService(BalanceRequestMapper requestMapper,
            BalanceResponseMapper responseMapper,
            WalletBalanceService walletBalanceService) {
        super(requestMapper, responseMapper, walletBalanceService);
    }

    public ResponseEntity<CommonVo> getBalance(CommonDto request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
