package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.queenmaker.dto.CreditTransactionsDto;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto {

    private String testmode;

    @NotEmpty(message = "Empty Array")
    private List<CreditTransactionsDto> transactions;
}



