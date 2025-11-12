package com.nextgen.gameaggregator.vendor.facai.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecryptionService;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FaChaiSignatureValidator extends AbstractVendorSignatureValidator {

    protected FaChaiSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                       VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.FACHAI.getClassName();
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        // run this logic only for promo payout
        return endpoint.contains(EndPoints.PROMO_PAYOUT);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String signature = formFields.get("Sign");
        String decrypted = formFields.get(VendorDecryptionService.KEY_DECRYPTED);

        checkSignature(signature, decrypted, "");
        return ValidationResult.success(Map.of(
                "paramsJsonString", decrypted
        ));
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        // TODO: return vendor specific error
        return new VendorErrorResponse(
                Map.of("error", "Invalid signature")
        );
    }
}
