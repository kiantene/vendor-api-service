package com.nextgen.gameaggregator.vendor.esoterica.api.result.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.*;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.request.CommonRequest;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@Service("betAndResultServiceForResult")
public class BetAndResultService extends AbstractBetResultController<CommonRequest, CommonResponse> {

    protected BetAndResultService(BetAndResultServiceRequestMapper requestMapper,
                                  BetAndResultServiceResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    public ResponseEntity<CommonResponse> result(@Valid @RequestBody CommonRequest request) {

//        Commented processRequest, as all the Esoterica gameCode that calls this endpoint are disabled, may refer to OVI-1571 comments.
//        Returning an INTERNAL_ERROR_NO_RETRY error response as a safety measure for any unintended calls to this endpoint.

        //return ResponseEntity.ok(processRequest(request));

        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCash(BigDecimal.ZERO);
        commonResponse.setBonus(BigDecimal.ZERO);
        commonResponse.setError(ResponseCodes.INTERNAL_ERROR_NO_RETRY.getCode());
        commonResponse.setDescription(ResponseCodes.INTERNAL_ERROR_NO_RETRY.getMessage());

        return ResponseEntity.ok(commonResponse);
    }

    @Override
    protected void configure(BetResultConfig config, CommonRequest request) {
        config.betAndResult(true)
                .setSettleType(SettleType.BET);
    }
}
