package com.nextgen.gameaggregator.vendor.jdb.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.Vendors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JdbValidator extends AbstractVendorSignatureValidator {

    protected JdbValidator(VendorPlayerDataService vendorPlayerDataService,
                           VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.JDB.getClassName();
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        // run this logic only for promo payout
        return true;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> parsedFields, String rawBody) throws SignatureValidationException {
        return ValidationResult.success(parsedFields);
    }
}
