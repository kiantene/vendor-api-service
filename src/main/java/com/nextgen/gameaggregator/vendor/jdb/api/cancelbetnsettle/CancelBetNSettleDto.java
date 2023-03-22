package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetNSettleDto {
    private Integer action;
    @Size(min = 13, max = 13)
    private Long ts;
    private String transferId;
    @Size(min = 13, max = 13)
    private String uid;
}
