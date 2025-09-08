package com.nextgen.gameaggregator.game.launcher.winfinity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameLaunchRequest {

    @Data
    @Builder
    public static class User {

        private String partnerSiteId;
        private String userId;
        private String language;
        private String timeZoneOffset;

    }

    private User user;
    private String currency;
    private String country;
    private String tableId;
    private String device;
    private String ipAddress;


}
