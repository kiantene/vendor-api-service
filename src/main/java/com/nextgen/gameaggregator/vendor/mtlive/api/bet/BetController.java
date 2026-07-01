package com.nextgen.gameaggregator.vendor.mtlive.api.bet;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
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
    private final VendorLineService vendorLineService;

    protected BetController(BetRequestMapper requestMapper,
                            BetResponseMapper responseMapper,
                            WalletBetService walletBetService,
                            VendorLineService vendorLineService) {
        super(requestMapper, responseMapper, walletBetService);
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.BET)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<String> bet(
            @Valid @ModelAttribute BetRequest request) {
        SuccessResponse response = processRequest(request);
        VendorCredentialAccessor accessor;
        try {
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.SYSTEM_CODE, request.getSystem_code());
            accessor = new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException(Credentials.SYSTEM_CODE + " not found", ex);
        }
        return VendorUtil.encryptResponse(response, accessor);
    }
}
