package com.nextgen.gameaggregator.vendor.playngo.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.playngo.api.result.JackpotDto;
import com.nextgen.gameaggregator.vendor.playngo.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "cancelReserve")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelReserveDto extends CommonDto implements RollbackData {

    @NotBlank
    @Size(min = 1, max = 64)
    @JacksonXmlProperty(localName = "externalId")
    private String externalId;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "transactionId")
    private String transactionId;

    @Pattern(regexp = "^(0|1)$")
    @JacksonXmlProperty(localName = "retry")
    private String retry;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "real")
    private BigDecimal real;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[a-zA-Z]+$")
    @JacksonXmlProperty(localName = "currency")
    private String currency;

    @NotBlank
    @Size(min = 1, max = 32)
    @JacksonXmlProperty(localName = "gameSessionId")
    private String gameSessionId;

    @NotNull
    @PositiveOrZero
    @JacksonXmlProperty(localName = "roundId")
    private Long roundId;

    @NotBlank
    @Pattern(regexp = "^(1|2|5)$")
    @JacksonXmlProperty(localName = "channel")
    private String channel;

    @Size(max = 32)
    @JacksonXmlProperty(localName = "freegameExternalId")
    private String freeGameExternalId;

    @Positive
    @Digits(integer = 13, fraction = 2)
    @JacksonXmlProperty(localName = "actualValue")
    private BigDecimal actualValue;

    @Nullable
    @JacksonXmlProperty(localName = "jackpots")
    @JacksonXmlElementWrapper(localName = "jackpots")
    private List<JackpotDto> jackpots;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.transactionId);
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }
}
