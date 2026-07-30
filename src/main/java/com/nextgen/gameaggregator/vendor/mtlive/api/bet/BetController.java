package com.nextgen.gameaggregator.vendor.mtlive.api.bet;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetController extends AbstractBetController<BetRequest, SuccessResponse> {
    private final VendorUtil vendorUtil;

    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletBetService,
                            VendorUtil vendorUtil) {
        super(requestMapper, responseMapper, walletBetService);
        this.vendorUtil = vendorUtil;
    }

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<String> bet(
            @Valid @ModelAttribute BetRequest request) {
        SuccessResponse response = processRequest(request);
        return vendorUtil.encryptResponse(response, request.getUser_id());
    }
}
