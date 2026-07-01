package com.nextgen.gameaggregator.vendor.mtlive.api.rollback;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.AbstractBetRollbackController;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackConfig;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
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
public class RollbackController extends AbstractBetRollbackController<RollbackRequest, SuccessResponse> {
    private final VendorLineService vendorLineService;

    public RollbackController(RollbackRequestMapper requestMapper,
                              RollbackResponseMapper responseMapper,
                              WalletRollbackServiceWrapper walletService,
                              VendorLineService vendorLineService) {
        super(requestMapper, responseMapper, walletService);
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.ROLLBACK)
    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<String> rollback(
            @Valid @ModelAttribute RollbackRequest request) {
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

    @Override
    public void configure(BetRollbackConfig config, RollbackRequest request) {
        config.rollbackType(RollbackType.BY_BET).allowRollbackForSettledBet(true);
    }

}
