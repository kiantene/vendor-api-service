package com.nextgen.gameaggregator.vendor.ag.event.eventrollback;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Record")
public class CommonEventRollBackDto implements RollbackData {

    @JacksonXmlProperty(localName = "Record")
    private EventRollBackDto eventRollBackDto;

    @Override
    public String getRollbackId() {
        return this.eventRollBackDto.getTransactionID();
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
