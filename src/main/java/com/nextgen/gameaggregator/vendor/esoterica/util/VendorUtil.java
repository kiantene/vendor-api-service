package com.nextgen.gameaggregator.vendor.esoterica.util;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.esoterica.response.ErrorResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

public class VendorUtil {

    private VendorUtil() {
        /* This utility class should not be instantiated */
    }

    public static String prepareRequest(Map<String, Object> params, String secretKey, String prefix) {
        try {
            Map<String, Object> sortedParams = new TreeMap<>();
            StringBuilder sb = new StringBuilder();

            // TreeMap automatically sorts keys alphabetically
            // Include all key except key="hash" and value that are not null
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String finalKey = "";

                if ((key == null || key.isBlank() || value == null || value.equals(""))) continue;

                if (prefix != null) {

                    if (key.startsWith(prefix + ".")) {

                        // handle bet.amount, bet.gameName case (bet body)
                        finalKey = key.substring(prefix.length() + 1);
                    }
                }
                else {
                    // skip any nested keys just in case
                    if (!key.contains(".")) {

                        // handle normal request body
                        finalKey = key;
                    }
                }

                if (!StringConstants.HASH.equals(finalKey) && !finalKey.isBlank()) {
                    sortedParams.put(finalKey, value);
                }
            }

            // Build concatenated string
            for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                sb.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }

            return sb.append(secretKey).toString();
        }
        catch (Exception e) {
            throw new RuntimeException(StringConstants.ERROR_GENERATE_HASH, e);
        }
    }

    public static CommonResponse enrichPostProcessInvalidRequest(String transactionId,
                                                                 PlayerBalanceData balanceData,
                                                                 Map<String, Object> responseBody,
                                                                 boolean includeCurrency,
                                                                 boolean includeUsedPromo,
                                                                 String endpoint) {

        ErrorResponse response = new ErrorResponse();
        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        if (!endpoint.equals(EndPoints.ENDROUND)) {
            response.setTransactionId(transactionId != null && !transactionId.isBlank() ? transactionId : "");
        }

        if (!endpoint.equals(EndPoints.REFUND)) {
            response.setCash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO);
            response.setBonus(BigDecimal.ZERO);
        }

        if (includeCurrency) {
            response.setCurrency(balanceData != null ? balanceData.getCurrency() : null);
        }

        if (includeUsedPromo) {
            response.setUsedPromo(balanceData != null ? BigDecimal.ZERO : null);
        }

        if (responseBody != null) {
            Object errorObj = responseBody.get(StringConstants.ERROR);
            response.setError(errorObj instanceof Number number ? number.intValue() : null);

            Object descriptionObj = responseBody.get(StringConstants.DESCRIPTION);
            response.setDescription(descriptionObj != null ? descriptionObj.toString() : null);
        }

        return response;
    }
}
