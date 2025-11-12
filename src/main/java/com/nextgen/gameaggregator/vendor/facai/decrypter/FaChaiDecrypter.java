package com.nextgen.gameaggregator.vendor.facai.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.security.encryption.EncryptionStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.decrypter.AbstractVendorDecrypter;
import com.nextgen.gameaggregator.core.security.decrypter.DecryptionResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class FaChaiDecrypter extends AbstractVendorDecrypter {
    private static final String PARAM_AGENT_CODE = "AgentCode";
    private static final String PARAM_ENCRYPTED_PARAMS = "Params";
    private record DecryptionParams(String agentCode, String encryptedParams) {}

    public FaChaiDecrypter(VendorPlayerDataService vendorPlayerDataService,
                           VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, EncryptionStrategyType.AES_ECB_PKCS5_BASE64);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.FACHAI.getClassName();
    }

    @Override
    public DecryptionResult doDecryption(HttpServletRequest request, Map<String, String> formFields, String rawBody) {
        DecryptionParams params = extractDecryptionParams(formFields);
        VendorCredentialAccessor accessor = getCredentialAccessorByKeyValue(
                Vendors.FACHAI.getId(),
                Credentials.AGENT_CODE,
                params.agentCode()
        );

        String secret = accessor.getValue(Credentials.AGENT_KEY);
        String decrypted = decrypt(params.encryptedParams, secret);
        return DecryptionResult.success(decrypted);
    }

    @Override
    public VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e) {
        // TODO: return vendor specific error
        return new VendorErrorResponse(
                Map.of("error", "Decryption failed")
        );
    }

    private DecryptionParams extractDecryptionParams(Map<String, String> formFields) {
        String agentCode = formFields.get(PARAM_AGENT_CODE);
        String encryptedParams = formFields.get(PARAM_ENCRYPTED_PARAMS);

        if (!StringUtils.hasText(agentCode)) {
            throw new DecryptionException("Missing or empty AgentCode parameter");
        }

        if (!StringUtils.hasText(encryptedParams)) {
            throw new DecryptionException("Missing or empty Params parameter");
        }

        return new DecryptionParams(agentCode, encryptedParams);
    }
}
