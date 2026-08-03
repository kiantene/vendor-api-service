package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * table_id and category are mutually exclusive by construction in VendorService.setPrdDto
 * (category-lobby launches use category, everything else that isn't a plain "type" launch uses
 * table_id) - these tests lock that invariant in so a future change can't silently set both.
 */
class PrdDtoTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private PrdDto validBase() {
        PrdDto dto = new PrdDto();
        dto.setId(1);
        dto.setIs_mobile(false);
        dto.setType(0);
        return dto;
    }

    @Test
    void typeOnly_hasNoViolations() {
        assertThat(validator.validate(validBase())).isEmpty();
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

        assertThat(violations).anyMatch(v -> v.getMessage().equals("table_id and category must not both be set"));
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
}
