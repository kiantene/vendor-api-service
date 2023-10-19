package com.nextgen.gameaggregator.vendor.iloveu.api.bet;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetDto {

    @NotEmpty(message = "Empty Array")
    private List<BetTransactionDto> transactions;

}
