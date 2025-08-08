package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;

public abstract class AbstractVendorSignatureValidator {
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorLineService vendorLineService;

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                               VendorLineService vendorLineService) {
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorLineService = vendorLineService;
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
}
