package com.nextgen.gameaggregator.exception;

public class InvalidOperatorResponseException extends Exception {

    private Integer operatorStatus = null;

    public InvalidOperatorResponseException() {
        super();
    }

    public InvalidOperatorResponseException(String message) {
        super(message);
    }

    public InvalidOperatorResponseException(Integer operatorStatus) {
        this.operatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Integer operatorStatus) {
        super(message);
        this.operatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Throwable cause, Integer operatorStatus) {
        super(message, cause);
        this.operatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(Throwable cause, Integer operatorStatus) {
        super(cause);
        this.operatorStatus = operatorStatus;
    }

    public InvalidOperatorResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer operatorStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.operatorStatus = operatorStatus;
    }

    public Integer getOperatorStatus() {
        return this.operatorStatus;
    }

    public void setOperatorStatus(Integer operatorStatus) {
        this.operatorStatus = operatorStatus;
    }
}
