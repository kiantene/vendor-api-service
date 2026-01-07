package com.nextgen.gameaggregator.vendor.gpkv2.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetResultService extends AbstractBetResultController<BetResultRequest, CommonVo> {

    public BetResultService(BetResultRequestMapper requestMapper,
            BetResultResponseMapper responseMapper,
            WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<CommonVo> result(BetResultRequest request) {
        return ResponseEntity.ok(processRequest(request,(context, resp)-> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {

        config.setSettleType(
                request.getBetTransactionId() == null ? SettleType.ROUND : SettleType.BET
        );
        config.betAndResult(false)
                .returnSuccessOnDuplicate(true);
    }

    private void enrichResponse(CommonVo response, BetResultRequest request) {
        response.setPlayer_id(request.getOperatorPlayerId());
    }
}