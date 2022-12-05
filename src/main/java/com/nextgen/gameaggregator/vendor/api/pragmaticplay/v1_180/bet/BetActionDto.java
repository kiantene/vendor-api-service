package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bet;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionDto extends AbstractActionDto {
    private String hash;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;
    private String gameId;
    private String roundId;
    private String amount;
    private String reference;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
    private String timestamp;
    private String roundDetails;
    private String token;
}
