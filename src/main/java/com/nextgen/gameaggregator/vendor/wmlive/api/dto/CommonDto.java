package com.nextgen.gameaggregator.vendor.wmlive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonDto {
    @NotBlank
    @Size(max = 255)
    private String cmd;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^(?!null$)(?!NULL$).*$")
    private String signature;

    @NotBlank
    @Size(max = 255)
    private String user;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")
    private String requestDate;
}
