package com.nextgen.gameaggregator.vendor.jdb.decrypter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.security.encryption.EncryptionStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.decrypter.AbstractVendorDecrypter;
import com.nextgen.gameaggregator.core.security.decrypter.DecryptionResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;

import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class JdbDecrypter extends AbstractVendorDecrypter {
    private static final String PARAM_X= "x";

    private record DecryptionParams(String encryptedParams) {}

    public JdbDecrypter(VendorPlayerDataService vendorPlayerDataService,
                        VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, EncryptionStrategyType.AES_CBC_NOPADDING_BASE64SAFE);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.JDB.getClassName();
    }

    @Override
    public DecryptionResult doDecryption(HttpServletRequest request, Map<String, String> formFields, String rawBody) {
        String encryptedParams = extractDecryptionParams(formFields);
        String path = Vendors.JDB.getCallback() + EndPoints.ACTION;
        String id = extractPathVariable(request, path,"id");

        VendorCredentialAccessor accessor = getCredentialAccessorByKeyValue(Vendors.JDB.getId(), Credentials.JDB_ID, id);
        String key = accessor.getValue(Credentials.KEY);
        String iv = accessor.getValue(Credentials.IV);
        try {
            String decrypted = decrypt(encryptedParams, key, iv);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> map = objectMapper.readValue(decrypted, new TypeReference<>(){});
            return DecryptionResult.success(decrypted, map);

        } catch (JsonProcessingException ex) {
            throw new DecryptionException();
        }
    }

    @Override
    public VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e) {
        return new VendorErrorResponse(
                Map.of("err_text", "Decryption failed")
        );
    }

    private String extractDecryptionParams(Map<String, String> formFields) {
        String x = formFields.get(PARAM_X);

        if (!StringUtils.hasText(x)) {
            throw new DecryptionException("Missing or empty encrypted param");
        }

        return x;
    }

    private String extractPathVariable(HttpServletRequest request, String path, String attributeName) {
        AntPathMatcher matcher = new AntPathMatcher();
        String pattern = path + "/{" + attributeName + "}/**";
        String uri = request.getRequestURI();

        Map<String, String> vars = matcher.extractUriTemplateVariables(pattern, uri);
        return vars.get(attributeName);
    }
}
