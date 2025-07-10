package com.nextgen.gameaggregator.game.launcher.saba;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameLaunchRequest {
    private String vendorId;
    private String vendorMemberId;
    private String platform;
}
