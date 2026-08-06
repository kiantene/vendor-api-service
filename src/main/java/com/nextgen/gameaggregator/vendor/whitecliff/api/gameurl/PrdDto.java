package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    // PositiveOrZero - not Positive. Not @NotNull: VendorService.setPrdDto's live-game branch
    // sets table_id or category instead and never touches type, so type is null on every such launch.
    @PositiveOrZero
    private Integer type;

    @Size(max = 255)
    private String table_id;

    @Size(max = 255)
    private String category;

    // NOTE: this constraint is NOT enforced automatically - GameUrlService.formDataBuilder
    // serializes this DTO via Gson with no @Valid/validator.validate() anywhere on the path, so
    // isExactlyOneOfTypeTableIdCategorySet() only runs when a test calls the validator directly.
    // The actual runtime guard is the explicit isExactlyOneOfTypeTableIdCategorySet() check inside
    // VendorService.setPrdDto, which throws InvalidFormatException before returning a bad DTO.
    //
    // Jackson auto-detects isXxx() as a getter for a "exactlyOneOfTypeTableIdCategorySet"
    // property - without @JsonIgnore this phantom boolean gets sent to the vendor on every
    // WhiteCliff launch request (this DTO is serialized as the outbound payload).
    @JsonIgnore
    @AssertTrue(message = "exactly one of type, table_id or category must be set")
    public boolean isExactlyOneOfTypeTableIdCategorySet() {
        int setCount = 0;
        if (type != null) {
            setCount++;
        }
        if (!isBlank(table_id)) {
            setCount++;
        }
        if (!isBlank(category)) {
            setCount++;
        }
        return setCount == 1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
