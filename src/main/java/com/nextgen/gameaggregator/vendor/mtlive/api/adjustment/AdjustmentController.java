package com.nextgen.gameaggregator.vendor.mtlive.api.adjustment;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.AbstractAdjustmentController;
import com.nextgen.gameaggregator.core.engine.wallet.adjustment.WalletAdjustmentServiceWrapper;
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
public class AdjustmentController extends AbstractAdjustmentController<AdjustmentRequest, SuccessResponse> {
    private final VendorUtil vendorUtil;

    public AdjustmentController(AdjustmentRequestMapper requestMapper,
                                AdjustmentResponseMapper responseMapper,
                                WalletAdjustmentServiceWrapper walletService,
                                VendorUtil vendorUtil) {
        super(requestMapper, responseMapper, walletService);
        this.vendorUtil = vendorUtil;
    }

    @PostMapping(path = EndPoints.ADJUSTMENT)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<String> result(
            @Valid @ModelAttribute AdjustmentRequest request) {
        SuccessResponse response = processRequest(request);
        return vendorUtil.encryptResponse(response, request.getUser_id());
    }
    
}
