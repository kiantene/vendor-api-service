package com.nextgen.gameaggregator.vendor.aasexyv2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    private String tableId;
    private String dealerDomain;
    private String winLoss;
    private String status;

}
