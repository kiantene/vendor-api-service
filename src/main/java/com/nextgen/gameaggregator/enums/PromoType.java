package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PromoType {
    FREE_ROUND(1, "FREEROUND", "Free Round"),
    TOURNAMENT(2, "TOURNAMENT", "Tournament");

    public final Integer id;
    public final String code;
    public final String description;

}
