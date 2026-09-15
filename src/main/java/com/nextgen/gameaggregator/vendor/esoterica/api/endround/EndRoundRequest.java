package com.nextgen.gameaggregator.vendor.esoterica.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.vendor.esoterica.util.StrictStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundRequest {

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
}
