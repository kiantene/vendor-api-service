package com.nextgen.gameaggregator.vendor.mtlive.api.betandresult;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequestMapping(path = EndPoints.PATH)
public class BetAndResultController extends AbstractBetResultController<BetAndResultRequest, SuccessResponse> {
    private final VendorUtil vendorUtil;

    public BetAndResultController(BetAndResultRequestMapper requestMapper,
                                  BetAndResultResponseMapper responseMapper,
                                  WalletBetResultServiceWrapper walletService,
                                  VendorUtil vendorUtil) {
        super(requestMapper, responseMapper, walletService);
        this.vendorUtil = vendorUtil;
    }

    @PostMapping(path = EndPoints.GIFT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<String> gift(
            @Valid @ModelAttribute BetAndResultRequest request) {
        SuccessResponse response = processRequest(request);
        return vendorUtil.encryptResponse(response, request.getUser_id());
    }

    @Override
    public void configure(BetResultConfig config, BetAndResultRequest request) {
        config.betAndResult(true).setSettleType(SettleType.BET);
    }
}
