package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exactly one of type/table_id/category is set by construction in VendorService.setPrdDto: the
 * live-game branch sets table_id or category and never touches type (so type is null on every
 * such launch - matches the OAS-5003 sample body {"id":1,"is_mobile":false,"table_id":"top_games"}
 * with no "type" key); every other launch sets only type. validBase() intentionally leaves all
 * three unset, mirroring the DTO's state before that branch runs, so no test accidentally
 * exercises a shape ("type=0" alongside table_id/category) production never produces.
 */
class PrdDtoTest {
    private static final String INVARIANT_MESSAGE = "exactly one of type, table_id or category must be set";

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private PrdDto validBase() {
        PrdDto dto = new PrdDto();
        dto.setId(1);
        dto.setIs_mobile(false);
        return dto;
    }

    @Test
    void typeOnly_hasNoViolations() {
        PrdDto dto = validBase();
        dto.setType(0);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void tableIdOnly_hasNoViolations() {
        PrdDto dto = validBase();
        dto.setTable_id("12345");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void categoryOnly_hasNoViolations() {
        PrdDto dto = validBase();
        dto.setCategory("12345");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void bothTableIdAndCategorySet_isRejected() {
        PrdDto dto = validBase();
        dto.setTable_id("12345");
        dto.setCategory("12345");

        Set<ConstraintViolation<PrdDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getMessage().equals(INVARIANT_MESSAGE));
    }

    @Test
    void typeAndTableIdBothSet_isRejected() {
        PrdDto dto = validBase();
        dto.setType(0);
        dto.setTable_id("12345");

        Set<ConstraintViolation<PrdDto>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getMessage().equals(INVARIANT_MESSAGE));
    }

    @Test
    void noneOfTypeTableIdCategorySet_isRejected() {
        // Matches what @NotNull on type used to catch, but for the right reason: nothing was
        // ever supposed to be entirely absent, not specifically "type must always be present".
        Set<ConstraintViolation<PrdDto>> violations = validator.validate(validBase());

        assertThat(violations).anyMatch(v -> v.getMessage().equals(INVARIANT_MESSAGE));
    }

    @Test
    void oversizedTableId_isRejected() {
        PrdDto dto = validBase();
        dto.setTable_id("x".repeat(256));

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void oversizedCategory_isRejected() {
        PrdDto dto = validBase();
        dto.setCategory("x".repeat(256));

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void blankTableIdAndCategory_withTypeSet_hasNoViolations() {
        // "" must mean "not set", same as null - but something else (type) must still be set,
        // since blank table_id/category don't count towards the exactly-one tally.
        PrdDto dto = validBase();
        dto.setType(0);
        dto.setTable_id("");
        dto.setCategory("");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void serializedJson_matchesActualProductionSerializer() {
        // GameUrlService.formDataBuilder ships this DTO via new Gson().toJson(...), not Jackson -
        // asserting against Gson's own output is what actually pins the real outbound payload
        // shape (Gson only serializes declared fields, so isExactlyOneOfTypeTableIdCategorySet()
        // was never at risk of leaking here regardless of any Jackson annotation). Matches the
        // OAS-5003 sample body exactly: {"id":1,"is_mobile":false,"table_id":"top_games"}.
        PrdDto dto = validBase();
        dto.setTable_id("12345");

        JsonObject json = new Gson().toJsonTree(dto).getAsJsonObject();

        assertThat(json.keySet()).containsExactlyInAnyOrder("id", "is_mobile", "table_id");
    }
}
