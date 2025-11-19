package com.nextgen.gameaggregator.vendor.mg.api.betresult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaDataDto {
    private Boolean isFreeGame;
    private FreeGame freeGame;
    private MetaDataProgressiveDto progressive;

    @Data
    @NoArgsConstructor
    public static class FreeGame {
        private Integer played;
        private Integer remaining;
        private String offerGuid;
        private String instanceGuid;
    }
}
