package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDto implements WinData {
    @NotBlank
    @Size(min = 1, max = 36)
    private String account;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;
    @NotBlank
    @Size(min = 1, max = 36)
    private String gamecode;
    @NotBlank
    @Size(min = 1, max = 30)
    private String roundid;
    private List<EndRoundDataDto> data;
    @NotBlank
    private String createTime;
    private BigDecimal freegame;
    private BigDecimal bonus;
    private BigDecimal luckydraw;
    private BigDecimal jackpot;
    private List<BigDecimal> jackpotcontribution;

    @Override
    public String getExternalTransactionId() {
        return this.getData().get(0).getMtcode();
    }

    @Override
    public BigDecimal getAmount() {
        return getData().get(0).getAmount();
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public Long getTimestamp() {
        Long timestamp;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date date = simpleDateFormat.parse(this.getData().get(0).getEventtime());
            timestamp = date.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }

    @Override
    public WinType getWinType() {
        return (this.getAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
    }
}
