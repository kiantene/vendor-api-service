package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class ClientResponseValidator {

    public void validate(ClientBalanceResponse response, RequestRecord request) throws InvalidOperatorResponseException {
        String traceId = String.valueOf(response.getTraceId());
        if (!request.traceId().equals(traceId)) {
            log.warn("Trace Id does not match (expected: {}) (actual: {})", request.traceId(), traceId);
        }

        String status = response.getStatus();
        if (status == null) {
            throw new InvalidOperatorResponseException("Status is empty");
        }

        // TODO: check eligible statuses

        PlayerBalanceData data = response.getData();
        if (data == null) {
            throw new InvalidOperatorResponseException("Data is empty");
        }

        String username = String.valueOf(data.getUsername());
        if (!request.username().equals(username)) {
            log.warn("[{}] Username does not match (expected: {}) (actual: {})",
                    request.traceId(),
                    request.username(),
                    username
            );
        }

        String currency = String.valueOf(data.getCurrency());
        if (!request.currency().equals(currency)) {
            log.warn("[{}] Currency does not match (expected: {}) (actual: {})",
                    request.traceId(),
                    request.currency(),
                    currency
            );
        }

        BigDecimal balance = data.getBalance();
        if (balance == null) {
            log.warn("[{}] Balance is empty", request.traceId());
            throw new InsufficientBalanceException("Balance is empty");
        }

        if (balance.signum() < 0) {
            throw new InsufficientBalanceException("Balance is " + balance.toPlainString());
        }
    }

    public record RequestRecord(String traceId, String username, String currency) {}
}
