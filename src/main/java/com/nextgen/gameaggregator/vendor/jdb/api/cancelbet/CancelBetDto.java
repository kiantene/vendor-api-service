package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelBetDto {
    private Integer action;
    @Size(min = 13, max = 13)
    private Long ts;
    private String transferId;
    @Size(min = 13, max = 13)
    private String uid;
    private String currency;
    private BigDecimal amount;
    private List<Long> refTransferIds;
    private Long gameRoundSeqNo;
}
