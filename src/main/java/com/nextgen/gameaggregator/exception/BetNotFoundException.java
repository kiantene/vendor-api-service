package com.nextgen.gameaggregator.exception;

public class BetNotFoundException extends Exception {

    private static int skipProcessGenerateBetStatus = 0;
    public BetNotFoundException() {
        super();
    }

    public BetNotFoundException(String message) {
        super(message);
    }

    public BetNotFoundException(int customStatus) {
        super();
        setSkipProcessGenerateBetStatus(customStatus);
    }

    public static void setSkipProcessGenerateBetStatus(int newStatus) {
        skipProcessGenerateBetStatus = newStatus;
    }

    public static int getSkipProcessGenerateBetStatus() {
        return skipProcessGenerateBetStatus;
    }
}
