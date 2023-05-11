package com.nextgen.gameaggregator.vendor.bng.api.authenticate;

import lombok.Data;

@Data
public class PlayerVo {
    private String id;      // Identifier of the user within the Casino Operator’s system
    private String brand;    // Currency of the player
    private String currency;    // Real balance of the player
    private String mode;       // Token/session of the player
    private Boolean is_test;

}
