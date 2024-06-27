package com.nextgen.gameaggregator.vendor.mg.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlDto {
    private String contentCode;
    private String platform;
    private String langCode;
}
