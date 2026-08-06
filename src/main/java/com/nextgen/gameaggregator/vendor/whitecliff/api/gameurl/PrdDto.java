package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrdDto {

    // id is a real product id (VendorService.setPrdDto: Integer.valueOf(productId)), never 0.
    @NotNull
    @Positive
    private Integer id;

    private Boolean is_mobile;

    // type=0 is a legitimate value (VendorService.setPrdDto's default lobbyCode is "0"), so
    // PositiveOrZero - not Positive - matches the established typeOnly_hasNoViolations case.
    @NotNull
    @PositiveOrZero
    private Integer type;

    @Size(max = 255)
    private String table_id;

    @Size(max = 255)
    private String category;

    @AssertTrue(message = "table_id and category must not both be set")
    public boolean isTableIdAndCategoryMutuallyExclusive() {
        return table_id == null || category == null;
    }
}
