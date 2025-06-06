package com.nextgen.gameaggregator.vendor.playngo.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Default {

    DEFAULT_LANGUAGE(3, "English"),
    DEFAULT_PLATFORM(2, "WEB");

    public final Integer id;
    public final String description;
}
