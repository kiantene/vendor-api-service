package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bet;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionDto extends AbstractActionDto {
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private String amount;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String reference;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;

    @Positive(message = ConstantErrorMessage.POSITIVE)
    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private Long timestamp;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundDetails;

    private String token;
}
