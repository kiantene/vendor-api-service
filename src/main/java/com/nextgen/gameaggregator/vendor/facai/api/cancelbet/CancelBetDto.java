package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto implements RollbackData {

    @NotBlank(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    public String BankID;

    @NotBlank(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @Size(min = 3, max = 4, message = ResponseCodes.TRANSACTION_NOT_EXIST)
    public String Currency;

    @NotBlank(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @Size(min = 2, max = 30, message = ResponseCodes.TRANSACTION_NOT_EXIST)
    public String MemberAccount;

    @PositiveOrZero(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @NotNull(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    public Integer GameID;

    @NotNull(message = ResponseCodes.TRANSACTION_NOT_EXIST)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.TRANSACTION_NOT_EXIST)
    public Long Ts;

    @Override
    public String getRollbackId() {
        return this.BankID;
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
