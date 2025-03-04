package com.nextgen.gameaggregator.vendor.smartsoft.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.wmlive.api.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralActionDto extends CommonDto {

    // Transaction ID (加扣点进行时我方交易单号，因失败回滚)
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    @Pattern(regexp = "^(?!null$)(?!NULL$).*$")
    private String dealid;

    // Game type (此游戏项目)
    // 101: 百家乐, 102: 龙虎, 103: 轮盘, 104: 骰宝, 105: 牛牛, 107: 番摊, 108: 色碟
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^(?!null$)(?!NULL$).*$")
    private String gtype;

    // Amount to be rolled back (回滾的金額，正數代表派彩回滾，負數代表下注回滾)
    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    // Game project, period, and point type (游戏项目_期数_局号_加扣点类型)
    // Example: 101_112139999_88_2
    // 1: 加点 2: 扣点 cancel: 取消
    @NotBlank
    private String type;

    // Game project, period, and game number (游戏项目_期数_局号)
    // Example: 101_112139999_88
    // cancel: 取消
    // Example: 101_112139999_88_cancel
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Pattern(regexp = "^(?!null$)(?!NULL$).*$")
    private String gameno;

    // Point type (加扣点类型)
    // 0: 电子游戏结算, 1: 加点, 2: 扣点, 3: 重对加点, 4: 重对扣点, 5: 重新派彩
    @NotNull
    private Integer code;

    @NotBlank
    private String category;


}
