package com.nextgen.gameaggregator.controller.vendorgame.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HeaderName {

    GAME_NAME ("GAME\nNAME"),
    OPEN_GAME_CODE ("OPEN\nGAME\nCODE"),
    SQUARE_IMAGE_NAME ("SQUARE\nIMAGE\nNAME"),
    SUPPORT_LANGUAGE ("SUPPORT\nLANGUAGE"),
    SUPPORT_CURRENCY ("SUPPORT\nCURRENCY"),
    SUPPORT_PLATFORM ("SUPPORT\nPLATFORM"),
    BET_GAME_CODE ("BET\nGAME\nCODE"),
    ;


    public final String name;
}
