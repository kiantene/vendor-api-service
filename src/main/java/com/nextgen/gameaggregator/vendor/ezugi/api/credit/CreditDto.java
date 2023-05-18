package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import lombok.Data;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditDto extends CommonDto {
}
