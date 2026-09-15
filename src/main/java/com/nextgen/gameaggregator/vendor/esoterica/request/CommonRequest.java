package com.nextgen.gameaggregator.vendor.esoterica.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.esoterica.util.BigDecimalDeserializer;
import com.nextgen.gameaggregator.vendor.esoterica.util.StrictStringDeserializer;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRequest {

    @NotBlank
    @Size(max = 255)
    private String hash;

    @NotBlank
    @Size(max = 255)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String userId;

    @NotBlank
    @Size(max = 255)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String gameName;

    @NotNull
    @Pattern(regexp = "^[0-9]+$")
    private String roundId;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 19, fraction = 0)
    @JsonDeserialize(using = BigDecimalDeserializer.class)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String reference;

    private Long timestamp;
}
