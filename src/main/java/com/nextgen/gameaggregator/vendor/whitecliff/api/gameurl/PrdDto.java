package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    // Plain method, not a Bean Validation constraint: nothing runs @Valid/validator.validate() on
    // this DTO in production (GameUrlService.formDataBuilder ships it via new Gson().toJson(...)),
    // so an @AssertTrue here would never fire outside a test calling the validator directly - that
    // would be decorative. The real runtime guard is VendorService.setPrdDto calling this method
    // directly and throwing InvalidFormatException if it returns false.
    //
    // No @JsonIgnore needed either: Gson's default reflective serialization only visits declared
    // fields, never methods (confirmed: new Gson().toJson(dto) never includes this method's value
    // under any name) - unlike Jackson, it does not auto-detect isXxx()/getXxx() as bean properties.
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
