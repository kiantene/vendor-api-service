package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.jackpotwin;

//import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JackpotWinActionDto extends AbstractActionDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;

    @Positive(message = ConstantErrorMessage.POSITIVE)
    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private Long timestamp;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String gameId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String roundId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String jackpotId;

    private String jackpotDetails;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private BigDecimal amount;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    private String reference;

    private String platform;

    private String token;
    
    // specPrizes[#].specPrizeType;
}
