package com.nextgen.gameaggregator.vendor.aasexyv2.api.balance;

import com.nextgen.gameaggregator.vendor.aasexyv2.dto.GeneralDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceDto extends GeneralDto {

    @NotBlank
    @Size(max = 50)
    private String userId;
}
