package com.nextgen.gameaggregator.enums;

public enum TxnStatus {
    /**
     * NEW status is when the request is first received and store minimum info in db for duplicate check
     */
    NEW,        // will block as duplicate

    /**
     * SENT status is when the request has been sent to Operator but have not received a response yet
     */
    SENT,       // will block as duplicate

    /**
     * PENDING status is when a bet result request is received, but no corresponding bet is found
     * Only applicable when BetResultConfig.allowResultBeforeBet is set to true
     */
    PENDING,    // will block as duplicate

    /**
     * SUCCESS status is when the request has been processed successfully by Operator
     */
    SUCCESS,    // will block as duplicate

    /**
     * ERROR status is when there is any error encountered
     */
    ERROR,      // will allow vendor resend

    /**
     * TIMEOUT status -> currently not in used.
     */
    TIMEOUT     // will allow vendor resend
    ;
}
