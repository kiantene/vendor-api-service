package com.nextgen.gameaggregator.vendor.dreamgaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDto {

    @NotBlank
    @Size(max = 255)
    private String username;

    @Digits(integer = 20, fraction = 8)
    private BigDecimal balance;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;
}
