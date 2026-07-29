package com.nextgen.gameaggregator.vendor.mtlive.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.mtlive.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MtliveSignatureValidator extends AbstractVendorSignatureValidator {

    private static final String ERR_INVALID_PARAM = "INVALID_PARAMETER";

    protected MtliveSignatureValidator(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, SigningStrategyType.MD5_REVERSE);
    }

    @Override
    public String getVendorClassName() {
        return MtliveConfig.CLASS_NAME;
    }

    @Override
    public ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String formattedFormFields = formFields.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("\n"));
        log.info("MTLive Request Body: \n"+formattedFormFields+"\n\nRequest Header: \n" + getHeaders(request)+"\n\nRequest URI: \n" + request.getRequestURI());

        String signature = request.getHeader(Headers.API_SI);
        String clientId = request.getHeader(Headers.API_CI);
        String timestamp = request.getHeader(Headers.API_TS);
        if (signature == null || timestamp == null || clientId == null) {
            throw new SignatureValidationException("Missing required security headers");
        }

        VendorCredentialAccessor accessor = getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, clientId);
        String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new SignatureValidationException("Missing Credentials clientSecret");
        }

        checkSignature(signature, formFields.get("msg"), timestamp+clientSecret+clientId);

        ValidationUtils.isEquals(accessor.getValue(Credentials.SYSTEM_CODE), formFields.get("system_code"), () -> new SignatureValidationException(ERR_INVALID_PARAM));
        ValidationUtils.isEquals(accessor.getValue(Credentials.WEB_ID), formFields.get("web_id"), () -> new SignatureValidationException(ERR_INVALID_PARAM));

        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            throw new SignatureValidationException(ERR_INVALID_PARAM);
        }
        return ValidationResult.success(formFields);
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception, Map<String, String> formFields) {
        ErrorResponse response = new ErrorResponse(ResponseCode.DECRYPTION_ERROR);
        if (exception.getMessage().equals(ERR_INVALID_PARAM)) {
            response = new ErrorResponse(ResponseCode.INVALID_PARAMETER);
        }
        response.setTimestamp(Instant.now().getEpochSecond());
        VendorCredentialAccessor accessor = resolveAccessor(formFields);
        String rawEncryptedMsg = VendorUtil.encryptResponse(response, accessor).getBody();

        return new VendorErrorResponse(HttpStatus.OK, rawEncryptedMsg);
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    public String getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headersString = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersString.append(headerName)
                    .append(":")
                    .append(headerValue)
                    .append("\n");
        }
        return headersString.toString();
    }

    private VendorCredentialAccessor resolveAccessor(Map<String, String> formFields) {
        VendorCredentialAccessor accessor;
        try {
            accessor = getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.SYSTEM_CODE, formFields.get("system_code"));
        } catch (Exception e) {
            accessor =getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.WEB_ID, formFields.get("web_id"));
        }
        return accessor;
    }
}
