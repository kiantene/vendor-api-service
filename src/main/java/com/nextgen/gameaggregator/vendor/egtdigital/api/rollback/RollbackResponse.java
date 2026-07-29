package com.nextgen.gameaggregator.vendor.egtdigital.api.rollback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.egtdigital.vo.ResponseCommonVo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class RollbackResponse extends ResponseCommonVo {

    @NotBlank
    @Size(max = 255)
    @JsonProperty("casinoTransferId")
    private String casinoTransferId;
}
