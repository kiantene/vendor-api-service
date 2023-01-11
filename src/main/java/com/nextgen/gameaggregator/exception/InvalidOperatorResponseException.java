package com.nextgen.gameaggregator.exception;

public class InvalidOperatorResponseException extends Exception {

    private Integer OperatorStatus = null;

    public InvalidOperatorResponseException() {
        super();
    }

    public InvalidOperatorResponseException(String message) {
        super(message);
    }

    public InvalidOperatorResponseException(Integer operatorStatus) {
        OperatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Integer operatorStatus) {
        super(message);
        OperatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Throwable cause, Integer operatorStatus) {
        super(message, cause);
        OperatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(Throwable cause, Integer operatorStatus) {
        super(cause);
        OperatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer operatorStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        OperatorStatus = operatorStatus;
    }

    public Integer getOperatorStatus() {
        return this.OperatorStatus;
    }

}
