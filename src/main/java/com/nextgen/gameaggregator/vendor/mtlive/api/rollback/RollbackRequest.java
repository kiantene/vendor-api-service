package com.nextgen.gameaggregator.vendor.mtlive.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {
    @NotBlank
    private String msg;

    @NotBlank
    @Size(max = 10)
    private String system_code;

    @NotBlank
    @Size(max = 15)
    private String web_id;

    @NotBlank
    @Size(max = 20)
    private String user_id;

    @NotBlank
    @Size(max = 15)
    private String bet_sn;

    @AssertTrue(message = "bet_sn must not contain spaces")
    public boolean isBetSnValid() {
        return bet_sn != null && !bet_sn.contains(" ");
    }
}
