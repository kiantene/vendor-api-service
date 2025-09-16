package com.nextgen.gameaggregator.vendor.kypoker.constant;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RoomCode {

    MATCHING(1),
    BONUS(2),
    SINGLE(3),
    FISHING(4),
    SLOT(6);

    public final int code;


    public static RoomCode fromCode(int code) {
        for (RoomCode rc : values()) {
            if (rc.code == code) return rc;
        }
        throw new IllegalArgumentException();
    }



}
