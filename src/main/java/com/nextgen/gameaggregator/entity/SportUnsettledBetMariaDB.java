package com.nextgen.gameaggregator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;

@Entity
@Table(name = "sports_unsettled_bet")
@Data
@NoArgsConstructor
public class SportUnsettledBetMariaDB {
    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("external_transaction_id")
    private String externalTransactionId;

    @JsonProperty("vendor_bet_id")
    private String vendorBetId;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("vendor_game_id")
    private Integer vendorGameId;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("operator_status")
    private Integer operatorStatus;

    @JsonProperty("game_category_id")
    private Integer gameCategoryId;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("bet_amount")
    private BigDecimal betAmount;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("game_session_token")
    private String gameSessionToken;

    @JsonProperty("vendor_bet_time")
    private Long vendorBetTime;

    public SportUnsettledBetMariaDB(SportUnsettledBetCouchbase sportUnsettledBetCouchbase) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(sportUnsettledBetCouchbase, this);

        this.setId(sportUnsettledBetCouchbase.getBetId());
    }
}
