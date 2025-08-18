package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.signature.SignatureStrategy;
import com.nextgen.gameaggregator.core.signature.SigningStrategyType;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public abstract class AbstractVendorSignatureValidator implements VendorSignatureValidator {
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorLineService vendorLineService;
    private final SignatureStrategy signatureStrategy;

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                               VendorLineService vendorLineService) {
        this(vendorPlayerDataService, vendorLineService, null);
    }

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                               VendorLineService vendorLineService,
                                               SigningStrategyType strategyType) {
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorLineService = vendorLineService;
        this.signatureStrategy = (strategyType != null ? strategyType : SigningStrategyType.NO_OP).getStrategy();
    }

    protected String getCredentialValue(String username, String credentialName) {
        Integer vendorLineId = null;
        try {
            VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(username);
            vendorLineId = vendorPlayer.getVendorLineId();
            return vendorLineService.getCredentialValueByName(vendorLineId, credentialName);
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException("vendorLineId: " + vendorLineId + " : " + credentialName + " not found", ex);
        }
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        return true;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(
                Map.of("error", "Invalid signature")
        );
    }

    protected String sign(String payload, String secret) {
        return signatureStrategy.sign(payload, secret);
    }

    protected String sign(Object payload, String secret) {
        return signatureStrategy.sign(payload, secret);
    }
}
