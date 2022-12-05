package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.balance;

import javax.validation.constraints.*;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletBalanceActionDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;
    private String token;

}
