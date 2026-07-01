package com.nextgen.gameaggregator.vendor.pragmaticplayv2.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PragmaticPlaySignatureValidator extends AbstractVendorSignatureValidator {
    protected PragmaticPlaySignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                              VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signature = formFields.get("hash");
        String username = formFields.get("userId");

        String payload = generateQueryString(formFields);
        String secretKey = getCredentialValueByUsername(username, Credentials.SECRET_KEY);

        checkSignature(signature, payload, secretKey);
        return ValidationResult.success();
    }

    @Override
    public String getVendorClassName() {
        return Vendors.PRAGMATIC.getClassName();
    }

    private String generateQueryString(Map<String, String> params) {
        params.remove("hash");
        return params.keySet().stream().sorted()
                .map(key -> key + "=" + params.get(key))
                .collect(Collectors.joining("&"));
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        Set<String> promoEndpointSet = Set.of(Endpoints.PROMO, Endpoints.BONUS);
        return promoEndpointSet.stream().anyMatch(endpoint::contains);
    }
}
