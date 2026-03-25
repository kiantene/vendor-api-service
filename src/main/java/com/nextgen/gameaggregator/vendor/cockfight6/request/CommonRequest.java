package com.nextgen.gameaggregator.vendor.cockfight6.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRequest {

    @NotNull
    @JsonProperty("record_id")
    private Long recordId;

    @NotBlank
    @JsonProperty("external_player_id")
    private String playerName;

    @NotNull
    @Digits(integer = 12, fraction = 2)
    private BigDecimal change;

    @NotNull
    @JsonProperty(value = "req_type", required = true)
    private Integer reqType;

    @NotNull
    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("remark_bet")
    private RemarkBet bet;

    @JsonProperty("remark_settle")
    private RemarkSettle settle;

}
