package com.nextgen.gameaggregator.core.security.decrypter;

import com.nextgen.core.exception.DecryptionException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
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
    private final RequestParserService parserService;
    private final LogContextService logContextService;

    public boolean doDecryption(VendorDecrypter decrypter,
                                ResettableRequestWrapper request,
                                HttpServletResponse response) throws IOException {
        try {
            String rawBody = request.getCachedBody();
            Map<String, String> parsedFields = parserService.parse(request.getContentType(), rawBody);

            Map<String, String> additional = decrypter.doDecryption(request, parsedFields, rawBody);
            if (additional != null && !additional.isEmpty()) {
                request.enrichRequestFields(additional);
                additional.forEach(logContextService::debug); // log injected fields
            }
            return true;
        } catch (DecryptionException ex) {
            LogContextHolder.get().setException(ex);
            VendorErrorResponse err = decrypter.onDecryptionFailure(request, ex);
            if (err == null || err.getBody() == null) {
                err = ResponseUtil.createDefaultErrorResponse("no response from decrypter");
            }
            ResponseUtil.writeErrorResponse(response, err.getBody(), err.getStatusCode().value());
            return false;
        }
    }
}
