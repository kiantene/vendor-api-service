package com.nextgen.gameaggregator.vendor.tbp.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataDto {
    @NotEmpty
    private List<@Valid GameDto> data;
}
