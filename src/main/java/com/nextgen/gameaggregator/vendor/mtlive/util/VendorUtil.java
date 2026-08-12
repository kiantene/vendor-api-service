package com.nextgen.gameaggregator.vendor.mtlive.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
@Slf4j
public class VendorUtil {

    /**
     * Request attribute carrying the vendorLineId resolved during signature validation. A
     * server-side request attribute (never a form field / request parameter) so an attacker-
     * supplied raw-body value cannot shadow it — the response-encryption read is trusted by
     * construction, not by getParameter ordering.
     */
    public static final String RESOLVED_VENDOR_LINE_ATTR = "mtlive.resolvedVendorLineId";

    private final VendorPlayerDataService vendorPlayerDataService;
    private final VendorLineService vendorLineService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public VendorUtil(VendorPlayerDataService vendorPlayerDataService, VendorLineService vendorLineService) {
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.vendorLineService = vendorLineService;
    }

    public static String encrypt(String plainText, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "DES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    protected VendorCredentialAccessor getCredentialAccessorByVendorLineId(Integer vendorLineId) {
        return new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));
    }

    /**
     * Encrypts an outgoing MTLive response using credentials resolved from the given player username.
     */
    public ResponseEntity<String> encryptResponse(Object response, String username) {
        if (username == null || username.isBlank()) {
            throw new InternalServerException("Cannot encrypt response: Username is required but was null or blank.");
        }
        VendorCredentialAccessor accessor;
        try {
            Integer vendorLineId = resolveVendorLineId(username);
            accessor = getCredentialAccessorByVendorLineId(vendorLineId);
        } catch (Exception e) {
            log.error("Failed to resolve credentials while encrypting MTLive response", e);
            throw new InternalServerException("Failed to encrypt MT Live response", e);
        }
        // Outside the catch: the static encryptResponse throws its own InternalServerException
        // on encrypt failure; keeping it here avoids double-wrapping it as a credential error.
        return encryptResponse(response, accessor);
    }

    /**
     * Resolves the vendorLineId for a player. On the request hot path the signature validator
     * has already looked this player up and stashed the id as a trusted request attribute, so we
     * reuse it and skip a redundant DB/cache round-trip. When the attribute is absent (e.g. error
     * post-processing outside the validated flow) we fall back to a direct username lookup,
     * which also re-confirms the player still exists.
     */
    private Integer resolveVendorLineId(String username) {
        Object trusted = getRequestAttribute(RESOLVED_VENDOR_LINE_ATTR);
        if (trusted instanceof Integer vendorLineId) {
            return vendorLineId;
        }
        return vendorPlayerDataService.getByUsername(username).getVendorLineId();
    }

    private Object getRequestAttribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getAttribute(name);
        }
        return null;
    }

    /**
     * Encrypts response directly using a provided VendorCredentialAccessor.
     * Used by exception handlers (like onInvalidSignature/onPlayerNotFound) where username lookup is unavailable.
     */
    public static ResponseEntity<String> encryptResponse(Object response, VendorCredentialAccessor accessor) {
        try {
            long timeStamp = Instant.now().getEpochSecond();
            if (response instanceof SuccessResponse success) {
                success.setTimestamp(timeStamp);
            }
            String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);
            String clientId = accessor.getValue(Credentials.CLIENT_ID);
            String key = accessor.getValue(Credentials.DES_KEY);
            String iv  = accessor.getValue(Credentials.DES_IV);

            String encryptedResponse;
            try {
                encryptedResponse = encrypt(objectMapper.writeValueAsString(response), key, iv);
            } catch (Exception e) {
                throw new InternalServerException("Failed to serialize or encrypt response payload", e);
            }

            String signature = DigestUtils.md5Hex(timeStamp + clientSecret + clientId + encryptedResponse);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain")
                    .header(Headers.API_CI, clientId)
                    .header(Headers.API_SI, signature)
                    .header(Headers.API_TS, String.valueOf(timeStamp))
                    .body(encryptedResponse);

        } catch (Exception e) {
            throw new InternalServerException("Failed to encrypt MT Live response", e);
        }
    }
}