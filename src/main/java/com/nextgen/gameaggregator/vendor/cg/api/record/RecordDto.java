package com.nextgen.gameaggregator.vendor.cg.api.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordDto {

    @NotBlank
    @Size(max = 255)
    String channelId;
    @NotBlank
    @Size(max = 255)
    String mtcode;
    @NotBlank
    @Size(max = 255)
    String roundId;
    @NotBlank
    @Size(max = 50)
    String accountId;
}
