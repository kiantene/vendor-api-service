package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bonuswin;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class BonusWinActionVo extends AbstractActionVo {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String transactionId;

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String currency;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    @Min(value = 0L, message = ConstantErrorMessage.MIN_REQUIRED + " 0")
    private BigDecimal cash;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    @Min(value = 0L, message = ConstantErrorMessage.MIN_REQUIRED + " 0")
    private BigDecimal bonus;
}
