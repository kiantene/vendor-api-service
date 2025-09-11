package com.nextgen.gameaggregator.vendor.inout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto<T> {
    @NotBlank
    @Size(max = 255)
    private String action;

    @NotBlank
    @Size(max = 255)
    private String token;

    private String gameMode;

    @Valid
    @NotNull
    private T data;
}
