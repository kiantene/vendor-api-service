package com.nextgen.gameaggregator.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromoPayoutHistory {

    @Id
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("vendor_transaction_id")
    private String vendorTransactionId;

    @JsonProperty("campaign_uuid")
    private String campaignUuid;

    @JsonProperty("agent_player_id")
    private Long agentPlayerId;

    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;

    @JsonProperty("vendor_player_id")
    private Long vendorPlayerId;

    @JsonProperty("vendor_player_username")
    private String vendorPlayerUsername;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_code")
    private String vendorCode;

    @JsonProperty("vendor_line_id")
    private Integer vendorLineId;

    @JsonProperty("agent_id")
    private Integer agentId;

    @JsonProperty("master_agent_id")
    private Integer masterAgentId;

    @JsonProperty("house_id")
    private Integer houseId;

    @JsonProperty("currency_id")
    private Integer currencyId;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("payout_amount")
    private BigDecimal payoutAmount;

    @JsonProperty("promo_type")
    private Integer promoType;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("vendor_transaction_time")
    private Long vendorTransactionTime;
}
