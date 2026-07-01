package com.nextgen.gameaggregator.vendor.mtlive.decrypter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.security.encryption.EncryptionStrategyType;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.decrypter.AbstractVendorDecrypter;
import com.nextgen.gameaggregator.core.security.decrypter.DecryptionResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.game.launcher.mtlive.util.VendorUtil;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.mtlive.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MtliveDecrypter extends AbstractVendorDecrypter {
    private static final String MSG = "msg";

    private record DecryptionParams(String encryptedParams) {}

    public MtliveDecrypter(VendorPlayerDataService vendorPlayerDataService,
                           VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService, EncryptionStrategyType.NO_OP);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public DecryptionResult doDecryption(HttpServletRequest request, Map<String, String> formFields, String rawBody) {
        String formattedFormFields = formFields.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("\n"));
        log.info("MTLive Decrypter Request Body: \n"+formattedFormFields+"\n\n Raw Body:"+rawBody+"\n\nRequest URI: \n" + request.getRequestURI());

        String encryptedParams = extractDecryptionParams(formFields);

        VendorCredentialAccessor accessor;
        try {
            accessor = getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, request.getHeader(Headers.API_CI));
        } catch (Exception ex) {
            throw new DecryptionException("Error retrieving credentials");
        }
        String key = accessor.getValue(Credentials.DES_KEY);
        String iv  = accessor.getValue(Credentials.DES_IV);
        try {
            String decryptedRequest = VendorUtil.decrypt(encryptedParams, key, iv);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> map = objectMapper.readValue(decryptedRequest, new TypeReference<>(){});
            return DecryptionResult.success(decryptedRequest, map);

        } catch (Exception ex) {
            throw new DecryptionException("Unable to decrypt request.");
        }
    }

    @Override
    public VendorErrorResponse onDecryptionFailure(HttpServletRequest request, DecryptionException e) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.DECRYPTION_ERROR);
        errorResponse.setTimestamp(Instant.now().getEpochSecond());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    private String extractDecryptionParams(Map<String, String> formFields) {
        String msg = formFields.get(MSG);

        if (!StringUtils.hasText(msg)) {
            throw new DecryptionException("Missing or empty encrypted param");
        }

        return msg;
    }
}
