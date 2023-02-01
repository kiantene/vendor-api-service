package com.nextgen.gameaggregator.vendor.cq9.api.endround;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndRoundDataDto {
    @NotBlank
    @Size(min = 1, max = 70)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String mtcode;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String eventtime;

    public Long getTimestamp(){
        Long timestamp;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date date = simpleDateFormat.parse(this.eventtime);
            timestamp = date.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }
}
