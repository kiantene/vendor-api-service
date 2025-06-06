package com.nextgen.gameaggregator.vendor.aasexy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameInfoDto {
    private List<String> result;
    private String roundStartTime;
    private String winner;
    private String ip;
    private String odds;
    private String streamerId;
    private String tableId;
    private String dealerDomain;
    private String winLoss;
    private String status;

}
