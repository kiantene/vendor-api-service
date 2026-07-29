package com.nextgen.gameaggregator.vendor.wazdan.api.close;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloseRequest {

    @NotNull
    @Valid
    private User user;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @NotBlank
        @Size(max = 255)
        private String id;

        private String skinId;

        @NotBlank
        @Size(max = 255)
        private String token;
    }
}
