package com.nextgen.gameaggregator.vendor.megafair.api.rollback;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackRequest {
    @NotBlank
    @Size(max = 255)
    private String userId;

    @NotBlank
    @Size(max = 255)
    private String casinoTransactionId;

    @NotBlank
    @Size(max = 255)
    private String hash;

    @NotBlank
    @Size(max = 255)
    private String gameSessionId;

    @AssertTrue(message = "casinoTransactionId must not contain spaces")
    public boolean isCasinoTransactionIdValid() {
        return casinoTransactionId == null || !casinoTransactionId.contains(" ");
    }
}

