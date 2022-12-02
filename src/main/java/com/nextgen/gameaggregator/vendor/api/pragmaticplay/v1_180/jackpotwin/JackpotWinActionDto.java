package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.jackpotwin;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JackpotWinActionDto {
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;

    @Positive(message = ConstantErrorMessage.POSITIVE)
    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private Long timestamp;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String campaignId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String campaignType;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private BigDecimal amount;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private String currency;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private String reference;

    private String roundId;
    private String gameId;
    private String dataType;
}
