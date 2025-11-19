package com.nextgen.gameaggregator.core.security.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.security.encryption.EncryptionStrategy;
import com.nextgen.core.security.encryption.EncryptionStrategyType;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public abstract class AbstractVendorDecrypter implements VendorDecrypter {
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorLineService vendorLineService;
    private final EncryptionStrategy encryptionStrategy;

    protected AbstractVendorDecrypter(VendorPlayerDataService vendorPlayerDataService,
                                      VendorLineService vendorLineService,
                                      EncryptionStrategyType strategyType) {
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorLineService = vendorLineService;
        this.encryptionStrategy = (strategyType != null ? strategyType : EncryptionStrategyType.NO_OP).getStrategy();
    }

    protected String getCredentialValueByUsername(String username, String credentialName) {
        Integer vendorLineId = null;
        try {
            VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(username);
            vendorLineId = vendorPlayer.getVendorLineId();

            return vendorLineService.getCredentialValueByName(vendorLineId, credentialName);
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException("vendorLineId: " + vendorLineId + " : " + credentialName + " not found", ex);
        }
    }

    protected VendorCredentialAccessor getCredentialAccessorByKeyValue(Integer vendorId, String key, String value) {
        try {
            // TODO: need to include vendorId, otherwise it may return more than 1 records
            Integer vendorLineId = vendorLineService.getVendorLineIdListByNameAndValue(key, value);
            return new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException(key + " not found", ex);
        }
    }

    @Override
    public VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e) {
        return new VendorErrorResponse(
                Map.of("error", "Decryption failed")
        );
    }

    protected String decrypt(String plaintext, String secret) throws DecryptionException {
        return encryptionStrategy.decrypt(plaintext, secret);
    }

    protected String decrypt(String plaintext, String secret, String iv) throws DecryptionException {
        return encryptionStrategy.decrypt(plaintext, secret, iv);
    }
}
