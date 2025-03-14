package com.nextgen.gameaggregator.vendor.ag.event.constant;

import lombok.Getter;

@Getter
public enum EventCode {
    DEALER_TIPS("EV0001"),
    RED_POCKET("EV0003"),
    GAMBLE_GOD("EV0004"),
    HAPPY_NEW_YEARS("EV0005"),
    TASK_REWARD("EV0007"),
    RANKING_REWARD("EV0010"),
    EVENT_REWARD("EV0012");

    private final String code;

    EventCode(String code) {
        this.code = code;
    }

    public static boolean isInvalidEventID(String eventID) {
        for (EventCode eventCode : EventCode.values()) {
            if (eventCode.getCode().equals(eventID)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotDealerTips(String eventID) {
        return !DEALER_TIPS.getCode().equals(eventID);
    }

}