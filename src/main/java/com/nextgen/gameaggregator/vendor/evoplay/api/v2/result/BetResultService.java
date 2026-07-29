package com.nextgen.gameaggregator.vendor.evoplay.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BetResultService extends AbstractBetResultController<CallbackDto, ResponseVo> {

    public BetResultService(BetResultRequestMapper requestMapper,
                            BetResultResponseMapper responseMapper,
                            WalletBetResultServiceWrapper walletService
    ) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<ResponseVo> result(CallbackDto request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetResultConfig config, CallbackDto request) {

        config.setSettleType(SettleType.ROUND);
    }
}
