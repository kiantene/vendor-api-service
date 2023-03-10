package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.validation.constraints.Size;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecialGameDto {
    @Size(max = 20)
    private String type;

    private Integer count;

    private Integer sequence;
}
