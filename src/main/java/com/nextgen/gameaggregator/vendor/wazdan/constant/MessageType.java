package com.nextgen.gameaggregator.vendor.wazdan.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {

    NON_INTRUSIVE(1, "Non-intrusive message – doesn’t block game play"),
    INTRUSIVE_OK(2, "Intrusive message – blocks game play and requires player confirmation by clicking OK button"),
    INTRUSIVE_ACTION(3, "Intrusive message – blocks game play and requires player action on provided buttons");

    private final int code;
    private final String description;

    public static MessageType fromCode(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MessageType code: " + code);
    }
}
