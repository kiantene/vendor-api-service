package com.nextgen.gameaggregator.vendor.dblive.api.batchbalance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchParamDto {
    @NotEmpty
    @Size(max = 2048)
    private List<@NotBlank @Size(max = 50) String> loginNames;
}
