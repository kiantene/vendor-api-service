package com.nextgen.gameaggregator.vendor.aviatorstudio.validator;

import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AviatorStudioSignatureValidator implements VendorSignatureValidator {
    @Override
    public String getVendorClassName() {
        return "aviator";
    }

    @Override
    public boolean validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) {

        // TODO: add jwt verify logic
        return true;
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.SERVER_ERROR);

        return new VendorErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseVo);
    }
}
