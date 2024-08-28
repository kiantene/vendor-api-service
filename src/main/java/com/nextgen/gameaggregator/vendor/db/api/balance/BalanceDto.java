package com.nextgen.gameaggregator.vendor.db.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    @Size(max = 50)
    private String memberId;

}
