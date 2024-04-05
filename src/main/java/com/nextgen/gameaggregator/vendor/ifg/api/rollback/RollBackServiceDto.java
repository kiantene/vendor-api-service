package com.nextgen.gameaggregator.vendor.ifg.api.rollback;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.vendor.ifg.service.VendorService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "server")
public class RollBackServiceDto implements RollbackData {

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String session;

    @JacksonXmlProperty(isAttribute = true)
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}")
    private String time;

    @JacksonXmlProperty(localName = "refund")
    @NotNull
    private RefundDto refund;

    @Override
    public String getRollbackId() {
        return this.getRefund().getStorno().getId();
    }

    @Override
    public Long getVendorSettledTime() {
        return VendorService.getTimeStamp(this.getTime());
    }
}
