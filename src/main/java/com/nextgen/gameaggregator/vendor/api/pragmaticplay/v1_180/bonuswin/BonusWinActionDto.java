package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bonuswin;

//import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusWinActionDto extends AbstractActionDto {

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

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String bonusCode;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    private String token;

    // private String requestId;
    // specPrizes[#].specPrizeAmount
    // specPrizes[#].specPrizeCode
    // specPrizes[#].specPrizeType

}
