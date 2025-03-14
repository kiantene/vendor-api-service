package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class UserDto {
        @NotNull
        @Digits(integer = 50, fraction = 0)
        private BigInteger id;

        @NotBlank
        @Size(max = 50)
        private String name;

        @NotNull
        @Digits(integer = 20, fraction = 8)
        private BigDecimal balance;

        @NotBlank
        @Size(max = 255)
        private String language;

        @NotBlank
        @Size(max = 5)
        private String currency;

        @NotBlank
        @Size(max = 255)
        private String sid;
    }

