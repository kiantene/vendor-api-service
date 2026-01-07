package com.nextgen.gameaggregator.vendor.gpkv2.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.GameCategories;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class BetService extends AbstractBetController<BetRequest, CommonVo> {
    protected BetService(BetRequestMapper requestMapper,
            BetResponseMapper responseMapper,
            WalletBetService walletBetService) {
        super(requestMapper, responseMapper, walletBetService);
    }

    public ResponseEntity<CommonVo> bet(
            @Valid @RequestBody BetRequest request) {
        return ResponseEntity.ok(processRequest(request));
    }

    @Override
    public void configure(BetConfig config, BetRequest request) {
        config.returnSuccessOnDuplicate(true);
    }

}
