package com.nextgen.gameaggregator.vendor.alizegames.api.betNSettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto {

    private String token;
    private String username;
    private String currency;
    private String operatorId;
    private Long timestamp;
    private String gameCode;
    private String ip;
    private String info;
    private String result;
    private String hits;
    private Long betTime;
    private Long processedTime;
}
