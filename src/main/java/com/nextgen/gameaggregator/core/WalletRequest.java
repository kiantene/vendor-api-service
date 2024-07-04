package com.nextgen.gameaggregator.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import lombok.Data;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class WalletRequest {
    // Http Info
    private String traceId;
    private String requestBody;
    private String errorMessage;
    private Integer status;

    // Master data Ids
    protected Integer agentId;
    protected Long agentPlayerId;
    protected String operatorUsername;
    protected Integer currencyId;
    protected Integer vendorId;
    protected Integer vendorLineId;
    protected Long vendorPlayerId;
    protected String vendorPlayerUsername;
    protected Integer vendorGameId;
    protected Integer gameCategoryId;
    protected String vendorToken;

    // Bet Info
    protected String token;
    protected String transactionId;
    protected String externalTransactionId;
    protected String vendorBetId;
    protected String betId;
    protected String roundId;
    protected String vendorGameCode;
    protected String currencyCode;
    protected Integer betType;
    protected Integer resultType;
    protected BigDecimal betAmount;
    protected BigDecimal winAmount;
    protected BigDecimal jackpotAmount;
    protected BigDecimal effectiveTurnover;
    protected BigDecimal winLoss;
    protected BetStatus betStatus;
    protected Long vendorBetTime;
    protected Long vendorSettleTime;
    protected Long timestamp;
    protected List<MultipleBetDto> betIds;
    protected String newVendorBetId;
    protected String newRoundId;

    // Sports Bet Info
    protected BigDecimal newBetAmount;

    // Operator Info
    private String requestType;
    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String apiSecret;
    private String operatorEndpoint;
    private String operatorData;
    private String operatorResponse;
    private int operatorHttpStatusCode;
    private ResponseCodes.Status operatorResponseStatus;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String gameCode;

    // Metric
    private Long startTime;
    private Long endTime;
    private Long betStart;
    private Long betEnd;
    private Long operatorStart;
    private Long operatorEnd;

    public WalletRequest() {
        this.init(UUID.randomUUID().toString());
    }

    public WalletRequest(String traceId) {
        this.init(traceId);
    }

    public WalletRequest(WalletRequest walletRequest) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(walletRequest, this);

        // regenerate new uuids for new request
        this.traceId = UUID.randomUUID().toString();
        this.betId = this.traceId;
        this.transactionId = this.traceId;
    }

    private void init(String traceId) {
        this.traceId = traceId;
        this.betId = traceId;
        this.transactionId = traceId;
        this.startTime = System.currentTimeMillis();
        this.balanceBefore = BigDecimal.ZERO;
        this.balanceAfter = BigDecimal.ZERO;
        this.vendorSettleTime = System.currentTimeMillis();
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;

        if (this.balanceBefore == null && this.betAmount != null) {
            balanceBefore = balanceAfter.add(betAmount);
        }
    }
}
