package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class WalletBalanceDto {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String token;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String hash;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String providerId;
    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String userId;
}
