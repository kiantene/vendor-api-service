package com.nextgen.gameaggregator.core.security.decrypter;

import java.util.Map;

public record DecryptionResult(
        boolean success,
        String decryptedText,
        Map<String, String> injectedFields
) {
    public static DecryptionResult success(String decrypted, Map<String,String> fields) {
        return new DecryptionResult(true, decrypted, fields != null ? fields : Map.of());
    }

    public static DecryptionResult success(String decryptedText) {
        return new DecryptionResult(true, decryptedText, Map.of());
    }

    public static DecryptionResult failure() {
        return new DecryptionResult(false, null, Map.of());
    }
}
