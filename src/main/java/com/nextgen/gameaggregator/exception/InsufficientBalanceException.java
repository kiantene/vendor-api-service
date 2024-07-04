package com.nextgen.gameaggregator.exception;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import lombok.Getter;

@Getter
public class InsufficientBalanceException extends Exception {
    private final Integer operatorStatus;

    public InsufficientBalanceException() {
        super();
        this.operatorStatus = ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code;
    }

    public InsufficientBalanceException(String message) {
        super(message);
        this.operatorStatus = ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code;
    }
}
