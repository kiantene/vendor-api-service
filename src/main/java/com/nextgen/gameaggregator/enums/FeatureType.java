package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FeatureType {
    MAX_PAYOUT(1, "MAXPAYOUT", "Max Payout");

    public final Integer id;
    public final String code;
    public final String description;

}
