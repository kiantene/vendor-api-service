package com.nextgen.gameaggregator.vendor.evoplay.api.v2.bet;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetServiceWrapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;

@Service
public class BetServices extends AbstractBetController<CallbackDto, ResponseVo> {
    protected BetServices(BetRequestMapper requestMapper,
                          BetResponseMapper responseMapper,
                          WalletBetServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<ResponseVo> bet(CallbackDto request) {
        return ResponseEntity.ok(processRequest(request));
    }
}
