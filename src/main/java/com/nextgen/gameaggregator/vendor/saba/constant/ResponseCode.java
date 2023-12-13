package com.nextgen.gameaggregator.vendor.saba.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCode {
    SUCCESS("0", null),
    NO_SUCH_TICKET_CANCEL_BET_RETRY("504", "No Such Ticket"),
    SYSTEM_ERROR_RETRY("999", "System Error");

    public final String status;
    public final String message;
}
