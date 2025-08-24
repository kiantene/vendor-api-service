package com.nextgen.gameaggregator.core.security.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorDecryptionService {
    public static final String KEY_DECRYPTED = "_decrypted";
    private final LogContextService logContextService;

    public DecryptionResult doDecryption(VendorDecrypter decrypter,
                                         ResettableRequestWrapper request,
                                         HttpServletResponse response,
                                         Map<String, String> parsedFields) throws IOException {
        try {
            DecryptionResult result = decrypter.doDecryption(request, parsedFields, request.getCachedBody());
            var injectedFields = result.injectedFields();
            if (injectedFields != null && !injectedFields.isEmpty()) {
                result.injectedFields().forEach(logContextService::debug);
            }
            logContextService.debug(KEY_DECRYPTED, result.decryptedText());
            return result;
        } catch (DecryptionException ex) {
            handleException(decrypter, request, response, ex);
            return DecryptionResult.failure();
        }
    }

    private void handleException(VendorDecrypter decrypter,
                                 ResettableRequestWrapper request,
                                 HttpServletResponse response,
                                 DecryptionException ex) throws IOException {

        LogContextHolder.get().setException(ex);
        VendorErrorResponse err = decrypter.onDecryptionFailure(request, ex);
        if (err == null || err.getBody() == null) {
            err = ResponseUtil.createDefaultErrorResponse("no response from decrypter");
        }
        ResponseUtil.writeErrorResponse(response, err.getBody(), err.getStatusCode().value());
    }
}
