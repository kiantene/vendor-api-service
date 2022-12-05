package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.refund;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundActionDto extends AbstractActionDto {
    private String userId;
    private String reference;
    private String providerId;
    private String token;
}
