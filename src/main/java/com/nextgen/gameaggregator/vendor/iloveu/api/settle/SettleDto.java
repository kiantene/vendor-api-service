package com.nextgen.gameaggregator.vendor.iloveu.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto {

    @NotEmpty(message = "Empty Array")
    private List<SettleTransactionDto> transactions;
}
