package com.nextgen.gameaggregator.vendor.cq9.api.takeall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TakeAllDto implements BetData {
    @NotBlank
    @Size(min = 1, max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String account;

    @NotBlank
    private String eventTime;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gamehall;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gamecode;

    @NotBlank
    @Size(min = 1, max = 30)
    private String roundid;

    @NotBlank
    @Size(min = 1, max = 70)
    private String mtcode;

    @NotBlank
    private String session;

    private BigDecimal amount;

    @Override
    public String getExternalTransactionId() {
        return this.mtcode;
    }

    @Override
    public BigDecimal getAmount() {
        return this.amount;
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
            Date date = simpleDateFormat.parse(this.getEventTime());
            timestamp = date.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }
}
