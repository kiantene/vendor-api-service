package com.nextgen.gameaggregator.game.launcher.esoterica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;

@Data
public class GameLaunchResponse {
    private boolean error;
    private String message;
    private UrlResponse data;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UrlResponse {
        private String launchUrl;
    }
}
