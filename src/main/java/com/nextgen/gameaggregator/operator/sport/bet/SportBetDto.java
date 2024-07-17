package com.nextgen.gameaggregator.operator.sport.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SportBetDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private String gameCode;
    private String currency;
    private Integer betType;
    private Long timestamp;
    private List<MultipleBetDto> multipleBetIds;

    public SportBetDto(WalletRequest walletRequest, BigDecimal conversionRate) {
        this.traceId = walletRequest.getTraceId();
        this.username = walletRequest.getOperatorUsername();
        this.betId = walletRequest.getBetId();
        this.transactionId = walletRequest.getTransactionId();
        this.externalTransactionId = walletRequest.getExternalTransactionId();
        this.roundId = walletRequest.getRoundId();
        this.betType = walletRequest.getBetType();
        this.currency = walletRequest.getCurrencyCode();
        this.gameCode = walletRequest.getGameCode();
        this.timestamp = walletRequest.getVendorBetTime();

        if (conversionRate == null) conversionRate = BigDecimal.ONE;

        BigDecimal convertedBetAmount = walletRequest.getBetAmount().multiply(conversionRate);
        this.betAmount = new BigDecimal(convertedBetAmount.stripTrailingZeros().toPlainString());

        if (Objects.nonNull(walletRequest.getBetIds()) && !walletRequest.getBetIds().isEmpty()) {
            this.multipleBetIds = new ArrayList<>(walletRequest.getBetIds().size());

            for (MultipleBetDto betDto : walletRequest.getBetIds()) {
                this.multipleBetIds.add(new MultipleBetDto(betDto, conversionRate));
            }
        }
    }
}
