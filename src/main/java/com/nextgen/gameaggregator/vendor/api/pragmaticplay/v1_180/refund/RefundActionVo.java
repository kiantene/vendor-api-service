package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.refund;

import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant.ConstantErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.vo.AbstractActionVo;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class RefundActionVo extends AbstractActionVo {

    @NotBlank(message = ConstantErrorMessage.NOT_BLANK)
    private String transactionId;

}
