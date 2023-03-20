package com.nextgen.gameaggregator.vendor.jdb.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements WinData {
    private String action;
    private Long ts;
    private Long transferId;
    private String uid;
    private String currency;
    private BigDecimal amount;
    private List<Long> refTransferIds;
    private Long gameRoundSeqNo;
    private Long gameSeqNo;
    @JsonProperty("gType")
    private Integer gType;
    @JsonProperty("mType")
    private Integer mType;
    private String reportDate;
    private String gameDate;
    private String lastModifyTime;
    private BigDecimal bet;
    private BigDecimal validBet;
    private BigDecimal win;
    private BigDecimal netWin;
    private BigDecimal tax;
    private String sessionNo;

    @Override
    public String getExternalTransactionId() {
        return this.transferId.toString();
    }

    @Override
    public BigDecimal getAmount() {
        return this.win;
    }

    @Override
    public String getRoundId() {
        return this.gameRoundSeqNo.toString();
    }

    @Override
    public String getGameId() {
        return this.mType.toString();
    }

    @Override
    public Long getTimestamp() {
        return this.ts;
    }

    @Override
    public WinType getWinType() {
        return this.win.compareTo(BigDecimal.ZERO) > 0 ? WinType.WIN : WinType.LOSE;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.validBet;
    }

    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog) {
        return betResultLog;
    }
}
