package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvoplayBalanceIncreaseValidator {
    private static final String BALANCE_INCREASE = "balanceincrease";
    private static final String FREE_ROUNDS_WIN = "free_rounds_win";

    public void validateV1(com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        validate(
                request.getName(),
                request.getData(),
                request.getData() == null ? null : request.getData().getId(),
                request.getData() == null ? null : request.getData().getUser_id(),
                request.getData() == null ? null : request.getData().getType(),
                request.getData() == null ? null : request.getData().getEvent_id(),
                request.getData() == null ? null : request.getData().getCurrency(),
                request.getData() == null ? null : request.getData().getAmount()
        );
    }

    public void validateV2(com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        validate(
                request.getName(),
                request.getData(),
                request.getData() == null ? null : request.getData().getId(),
                request.getData() == null ? null : request.getData().getUser_id(),
                request.getData() == null ? null : request.getData().getType(),
                request.getData() == null ? null : request.getData().getEvent_id(),
                request.getData() == null ? null : request.getData().getCurrency(),
                request.getData() == null ? null : request.getData().getAmount()
        );
    }

    public boolean isFreeRoundWin(String action, String type) {
        return BALANCE_INCREASE.equalsIgnoreCase(trim(action))
                && FREE_ROUNDS_WIN.equalsIgnoreCase(trim(type));
    }

    private void validate(String name,
                          Object data,
                          String id,
                          String userId,
                          String type,
                          String eventId,
                          String currency,
                          String amount) {
        require(BALANCE_INCREASE.equalsIgnoreCase(trim(name)), "name must be BalanceIncrease");
        require(data != null, "data is required");
        requireNotBlank(id, "data.id is required");
        requireNotBlank(userId, "data.user_id is required");
        requireNotBlank(type, "data.type is required");
        require(isFreeRoundWin(name, type), "data.type must be free_rounds_win");
        requireNotBlank(eventId, "data.event_id is required for free_rounds_win");
        requireNotBlank(currency, "data.currency is required");
        requireNotBlank(amount, "data.amount is required");
        requireAmount(amount);
    }

    private void requireAmount(String amount) {
        try {
            require(new BigDecimal(amount).signum() >= 0, "data.amount must be positive or zero");
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("data.amount is invalid", ex);
        }
    }

    private void requireNotBlank(String value, String message) {
        require(!trim(value).isBlank(), message);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
