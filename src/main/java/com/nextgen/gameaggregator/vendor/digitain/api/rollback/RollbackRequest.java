package com.nextgen.gameaggregator.vendor.digitain.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {
    @NotNull
    @JsonProperty("prid")
    private Integer prid;

    @NotBlank
    @JsonProperty("pid")
    private String pid;

    //testing vendor giving integer
    @NotBlank
    @Size(max = 255)
    @JsonProperty("gid")
    private String gid;

    @NotNull
    @JsonProperty("refrnd")
    private Boolean refrnd;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("rid")
    private String rid;

    @Size(max = 255)
    @JsonProperty("otxid")
    private String otxid;

}
