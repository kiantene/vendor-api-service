package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.promowin;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromoWinActionDto {
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private BigDecimal amount;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String reference;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;

    @Positive(message = ConstantErrorMessage.POSITIVE)
    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private Long timestamp;

    //* Not mandatory
    private String bonusCode;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String token;

    //* Not mandatory
    private String requestId;

    // specPrizes[#].specPrizeAmount
    // specPrizes[#].specPrizeCode
    // specPrizes[#].specPrizeType
}
