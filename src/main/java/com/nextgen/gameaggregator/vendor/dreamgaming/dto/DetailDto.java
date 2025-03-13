package com.nextgen.gameaggregator.vendor.dreamgaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailDto {

    @NotBlank
    @Size(max = 255)
    private String ext;

    @Size(max = 255)
    private String parentBetId;
}
