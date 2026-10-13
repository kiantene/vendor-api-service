package com.nextgen.gameaggregator.vendor.megafair.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.request.CommonRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VendorUtil {

    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final WalletBalanceService walletBalanceService;
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final String DELIMITER = "\\|";

    public VendorUtil(Validator validator, ObjectMapper objectMapper, WalletBalanceService walletBalanceService) {
        this.validator = validator;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.walletBalanceService = walletBalanceService;
    }

    public <T> T convertDto(Object source, Class<T> objectClass) {
        if (source == null) {
            throw new InvalidRequestException("Source request object cannot be null");
        }

        T object;
        try {
            object = objectMapper.convertValue(source, objectClass);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new InvalidRequestException(errorMessage);
        }

        return object;
    }

    public void parseFormattedFields(CommonRequest request) {
        if (request == null) return;

        // 1. Handle Bet field (Format: bet_amount | transactionId)
        parseField(request.getBet(), "bet").ifPresent(fieldParts -> {
            request.setBetAmount(fieldParts.amount());
            request.setBetTransactionId(fieldParts.transactionId());
        });

        // 2. Handle Win field (Format: win_amount | transactionId)
        parseField(request.getWin(), "win").ifPresent(fieldParts -> {
            request.setWinAmount(fieldParts.amount());
            request.setWinTransactionId(fieldParts.transactionId());
        });
    }

    private Optional<FieldParts> parseField(String rawValue, String fieldName) {
        if (rawValue == null) {
            return Optional.empty();
        }

        if (rawValue.isBlank()) {
            throw new InvalidRequestException("Field '" + fieldName + "' must not be blank");
        }

        try {
            if (!rawValue.contains("|")) {
                throw new InvalidRequestException("Malformed field structure missing delimiter '|'");
            }

            String[] parts = rawValue.split(DELIMITER, -1);

            String amountStr = parts[0].trim();
            if (amountStr.isEmpty()) {
                throw new InvalidRequestException("Missing amount for field: " + fieldName);
            }

            BigDecimal amount = new BigDecimal(amountStr);

            // Reject negative amounts to enforce @PositiveOrZero equivalence during parsing
            if (amount.signum() < 0) {
                throw new InvalidRequestException("Amount must be positive or zero for field: " + fieldName);
            }

            // Strictly enforce integer in cents (disallow non-zero fractional decimals)
            if (amount.stripTrailingZeros().scale() > 0) {
                throw new InvalidRequestException("Amount must be an integer in cents for field: " + fieldName);
            }

            String txId = parts.length > 1 ? parts[1].trim() : "";
            if (txId.isEmpty()) {
                throw new InvalidRequestException("Missing transaction ID for field: " + fieldName);
            }

            return Optional.of(new FieldParts(amount, txId));
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse MegaFair formatted field '{}' with raw value '{}'", fieldName, rawValue, e);
            throw new InvalidRequestException("Invalid data format for field: " + fieldName, Collections.emptyMap(), e);
        }
    }

    private record FieldParts(BigDecimal amount, String transactionId) {}

    public VendorErrorResponse toXmlVendorResponse(HttpStatus status, Object body) {
        try {
            String xml = XML_MAPPER.writeValueAsString(body);
            return new VendorErrorResponse(status, xml, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE));
        } catch (JsonProcessingException e) {
            log.error("Megafair XML Serialization failed", e);
            return new VendorErrorResponse(status, body);
        }
    }

    public BigDecimal getPlayerBalanceDataByUserId(String userId) {
        //skip overwriting the balance when a wallet service lookup fails
        if (userId == null) {
            log.warn("Cannot fetch balance for duplicate request response: userId is null");
            return null;
        }

        try {
            BalanceContext balanceContext = BalanceContext.builder()
                    .vendorPlayerUsername(userId)
                    .build();

            var result = walletBalanceService.process(balanceContext);
            if (result != null && result.getBalance() != null) {
                return result.getBalance();
            }
        } catch (Exception e) {
            log.error("Failed to fetch player balance for duplicate request response (userId: {})", userId, e);
        }

        return null;
    }

}
