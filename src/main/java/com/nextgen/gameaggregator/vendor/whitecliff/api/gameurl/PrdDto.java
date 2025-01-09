package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrdDto {

    @NotNull
    @Digits(integer = 50, fraction = 0)
    private Integer id;

    private Boolean is_mobile;

    @NotNull
    @Digits(integer = 50, fraction = 0)
    private Integer type;

    @NotBlank
    @Size(max = 255)
    private String table_id;
}
