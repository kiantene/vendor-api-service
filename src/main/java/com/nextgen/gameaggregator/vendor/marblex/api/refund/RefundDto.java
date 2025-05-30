package com.nextgen.gameaggregator.vendor.marblex.api.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.sport.refund.SportRefundData;
import com.nextgen.gameaggregator.util.DateTimeConverter;
import com.nextgen.gameaggregator.vendor.marblex.constant.Formats;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundDto extends CommonDto implements SportRefundData {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("RoundID")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("JanusTransactionID")
    private String janusTransactionId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("PlatformTransactionID")
    private String platformTransactionID;

    @NotBlank
    @Pattern(regexp = Formats.TIME_REGEX)
    @JsonProperty("TransactionTime")
    private String transactionTime;

    @Override
    public String getExternalTransactionId() {
        return this.janusTransactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.janusTransactionId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.getPlayerId();
    }

    @Override
    public Long getTimestamp() {
        return DateTimeConverter.convertToTimestamp(this.transactionTime, DateTimeConverter.ISO_8601);
    }
}
