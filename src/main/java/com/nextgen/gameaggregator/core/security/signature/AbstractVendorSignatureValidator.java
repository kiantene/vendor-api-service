package com.nextgen.gameaggregator.core.security.signature;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SignatureStrategy;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public abstract class AbstractVendorSignatureValidator implements VendorSignatureValidator {
    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorLineService vendorLineService;
    private final GameSessionDataService gameSessionDataService;
    private final SignatureStrategy signatureStrategy;

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                            VendorLineService vendorLineService,
                                            GameSessionDataService gameSessionDataService) {
        this(vendorPlayerDataService, vendorLineService, gameSessionDataService, null);
    }

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                            VendorLineService vendorLineService) {
        this(vendorPlayerDataService, vendorLineService, null, null);
    }

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                            VendorLineService vendorLineService,
                                            SigningStrategyType strategyType) {
        this(vendorPlayerDataService, vendorLineService, null, strategyType);
    }

    protected AbstractVendorSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                            VendorLineService vendorLineService,
                                            GameSessionDataService gameSessionDataService,
                                            SigningStrategyType strategyType) {
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorLineService = vendorLineService;
        this.gameSessionDataService = gameSessionDataService;
        this.signatureStrategy = (strategyType != null ? strategyType : SigningStrategyType.NO_OP).getStrategy();
    }

    /**
     * @deprecated use getCredentialValueByUsername instead
     */
    @Deprecated
    protected String getCredentialValue(String username, String credentialName) {
        return getCredentialValueByUsername(username, credentialName);
    }

    protected String getCredentialValueByUsername(String username, String credentialName) {
        Integer vendorLineId = null;
        try {
            VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(username);
            vendorLineId = vendorPlayer.getVendorLineId();

            return vendorLineService.getCredentialValueByName(vendorLineId, credentialName);

        } catch (EntityNotFoundException ex) {
            throw new SignatureValidationException("Player not found: " + username, new PlayerNotFoundException());

        } catch (Exception ex) {
            throw new SignatureValidationException(ex.getMessage(), ex);
        }
    }

    protected VendorCredentialAccessor getCredentialAccessorByKeyValue(Integer vendorId, String key, String value) {
        try {
            // TODO: need to include vendorId, otherwise it may return more than 1 records
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(key, value);
            return new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
        } catch (CredentialNotFoundException ex) {
            throw new InternalConfigurationException(key + " not found", ex);
        }
    }

    protected VendorCredentialAccessor getCredentialAccessorByVendorLineId(Integer vendorLineId) {
        return new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
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

    protected final void checkSignature(String expectedSignature, String payload, String secret) {
        if (!expectedSignature.equals(sign(payload, secret))) {
            throw new SignatureValidationException("Signature does not match");
        }
    }

    // TODO: enhance to include VendorRequestContext
    protected final GameSession getGameSessionByToken(String token) {
        if (gameSessionDataService == null) {
            throw new IllegalStateException("GameSessionDataService is not initialised, use constructor (VendorPlayerDataService, VendorLineService, GameSessionDataService)");
        }
        return gameSessionDataService.getByToken(token, null);
    }

    protected final GameSession getGameSessionByVendorToken(String vendorToken) {
        if (gameSessionDataService == null) {
            throw new IllegalStateException("GameSessionDataService is not initialised, use constructor (VendorPlayerDataService, VendorLineService, GameSessionDataService)");
        }
        return gameSessionDataService.getByVendorToken(vendorToken, null);
    }
}
