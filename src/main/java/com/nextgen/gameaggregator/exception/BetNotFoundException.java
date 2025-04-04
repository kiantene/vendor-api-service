package com.nextgen.gameaggregator.exception;

public class BetNotFoundException extends Exception {

    private static Integer skipProcessGenerateBetStatus = 0;
    public BetNotFoundException() {
        super();
    }

    public BetNotFoundException(String message) {
        super(message);
    }

    public BetNotFoundException(Integer customStatus) {
        super();
        setSkipProcessGenerateBetStatus(customStatus);
    }

    public static void setSkipProcessGenerateBetStatus(Integer newStatus) {
        skipProcessGenerateBetStatus = newStatus;
    }

    public static Integer getSkipProcessGenerateBetStatus() {
        return skipProcessGenerateBetStatus;
    }
}
