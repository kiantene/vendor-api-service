package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.endround;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class EndRoundActionVo extends AbstractActionVo {

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    @Min(value = 0L, message = ConstantErrorMessage.MIN_REQUIRED + " 0")
    private BigDecimal cash;

    @NotNull(message = ConstantErrorMessage.NOT_NULL)
    @Min(value = 0L, message = ConstantErrorMessage.MIN_REQUIRED + " 0")
    private BigDecimal bonus;
}
