package com.nextgen.gameaggregator.sport.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SportSettledBet {
    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("vendor_bet_id")
    private String vendorBetId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("win_amount")
    private BigDecimal winAmount;

    @JsonProperty("effective_turnover")
    private BigDecimal effectiveTurnover;

    @JsonProperty("odds")
    private BigDecimal odds;

    @JsonProperty("odd_type_id")
    private Integer oddTypeId;

    @JsonProperty("vendor_bet_time")
    private Long vendorBetTime;

    @JsonProperty("result_time")
    private Long resultTime;

    @JsonProperty("vendor_settle_time")
    private Long vendorSettleTime;

    @JsonProperty("retry_count")
    private Integer retryCount;

    @JsonProperty("next_execution_time")
    private Long nextExecutionTime;

    @JsonProperty("raw_data")
    private String rawData;

    public SportSettledBet(SportBetResultData sportBetResultData, String rawData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(sportBetResultData, this);

        this.setRawData(rawData);
    }
}
