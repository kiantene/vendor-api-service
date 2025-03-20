package com.nextgen.gameaggregator.vendor.aglive.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Data")
public class CommonRefundDto implements RollbackData {

    @JacksonXmlProperty(localName = "Record")
    private RefundDto refundDto;

    @Override
    public String getRollbackId() {
        return this.refundDto.getTransactionID();
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
