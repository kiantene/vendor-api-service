package com.nextgen.gameaggregator.vendor.booongo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDto {
    @NotBlank
    private String id;

    @NotBlank
    private String currency;

    @NotBlank
    private String mode;

    @NotNull
    private Boolean is_test;

    @NotBlank
    private String brand;

    public Boolean getIs_test() {
        return is_test;
    }

    public void setIs_test(Boolean is_test) {
        this.is_test = is_test;
    }
}
