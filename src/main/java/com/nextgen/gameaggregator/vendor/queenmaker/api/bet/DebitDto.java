package com.nextgen.gameaggregator.vendor.queenmaker.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto {
    private String testmode;
    private Boolean transactional;
    private List<DebitTransactionsDto> transactions;
}
