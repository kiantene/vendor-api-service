package com.nextgen.gameaggregator.vendor.ag.api.rollback;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Data")
public class CommonRollBackDto implements RollbackData {

    @JacksonXmlProperty(localName = "Record")
    private RollBackDto rollBackDto;


    @Override
    public String getRollbackId() {
        return this.rollBackDto.getTransactionID();
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