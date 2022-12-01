package com.nextgen.gameaggregator.vendor.api.vendor.pragmaticplay.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeamlessGameLoginResponseDto {
    private String error;
    private String description;
    private String gameURL;

    public SeamlessGameLoginResponseDto(String error, String description, String gameURL) {
        this.error = error;
        this.description = description;
        this.gameURL = gameURL;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGameURL() {
        return gameURL;
    }

    public void setGameURL(String gameURL) {
        this.gameURL = gameURL;
    }
}
