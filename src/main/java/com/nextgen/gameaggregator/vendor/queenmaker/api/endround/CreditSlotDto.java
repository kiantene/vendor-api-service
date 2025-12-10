package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditSlotDto {
    private String testmode;

    @NotEmpty(message = "Empty Array")
    private List<CreditSlotTransactionsDto> transactions;
}
