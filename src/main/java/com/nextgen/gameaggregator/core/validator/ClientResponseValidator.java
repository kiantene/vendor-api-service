package com.nextgen.gameaggregator.core.validator;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ClientResponseValidator {

    public void validate(ClientApiResponse response, RequestRecord request, VendorRequestContext context) throws InvalidOperatorResponseException {
        String status = response.getStatus();
        if (status == null) {
            throw new InvalidOperatorResponseException("Status is empty");
        }

        String traceId = response.getTraceId();
        if (!request.traceId().equals(traceId)) {
            log.warn("Trace Id does not match (expected: {}) (actual: {})", request.traceId(), traceId);
        }

        // TODO: check eligible statuses
        if (ResponseCodes.Status.isInsufficientFunds(response.getStatus())) {
            throw new InsufficientBalanceException(context, "Insufficient Balance");
        }

        PlayerBalanceData data = response.getData();
        if (data == null) {
            log.error("[{}] {}: Data is empty", traceId, request.type());
            throw new InvalidOperatorResponseException("Data is empty");
        }

        String username = data.getUsername();
        if (!request.username().equals(username)) {
            log.warn("[{}] {}: Username does not match (expected: {}) (actual: {})",
                    traceId,
                    request.type(),
                    request.username(),
                    username
            );
        }

        String currency = data.getCurrency();
        if (!request.currency().equals(currency)) {
            log.warn("[{}] {}: Currency does not match (expected: {}) (actual: {})",
                    traceId,
                    request.type(),
                    request.currency(),
                    currency
            );
        }

        if (request.validateBalance()) {
            BigDecimal balance = data.getBalance();
            if (balance == null) {
                throw new InsufficientBalanceException(context, "Balance is empty");
            }

            if (balance.signum() < 0) {
                throw new InsufficientBalanceException(context, "Balance is " + balance.toPlainString());
            }
        }
    }

    public record RequestRecord(String type, String traceId, String username, String currency, boolean validateBalance) {}
}
