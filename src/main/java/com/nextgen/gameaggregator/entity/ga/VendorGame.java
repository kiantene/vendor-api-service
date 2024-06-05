package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;
import java.util.Optional;

@Entity
@Table(name = "vendor_games")
@Data
public class VendorGame extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code;
    private String vendorGameCode;
    private String name;

    private Integer betDataPreprocessing;

    private Integer gameCategoryId;
    private Integer vendorId;

    private Integer isByCurrency;
    private String imageSquare;
    private String imageLandscape;
    private Integer status;

    
    @Document
    @Scope("raw")
    @Collection("pinnacle_vendor_username")
    @Data
    public static class PinnacleVendorPlayer {
        @Id
        private String id; // couchbase primary key
        private String vendorPlayerUsername;
        private String username;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RawBatchProcessIdempotentLog {
        private String id;
        private String action;
        private String url;
    }

    @Entity
    @Table(name = "sports_unsettled_bet")
    @Data
    @NoArgsConstructor
    public static class SportUnsettledBetMariaDB {
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

        @JsonProperty("bet_type")
        private Integer betType = 1;

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

        @JsonProperty("create_date")
        private Long createDate = System.currentTimeMillis();

        @JsonProperty("resettle_num")
        private Integer resettleNum = 0;

        public SportUnsettledBetMariaDB(SportUnsettledBetCouchbase sportUnsettledBetCouchbase) {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            modelMapper.map(sportUnsettledBetCouchbase, this);

            this.setId(sportUnsettledBetCouchbase.getBetId());
            Optional.ofNullable(sportUnsettledBetCouchbase.getNewBetAmount()).ifPresent(this::setBetAmount);
        }
    }
}
