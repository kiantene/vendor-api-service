package com.nextgen.gameaggregator.vendor.api.pgsoft.v2_4_4.cashtransferinout;

import com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant.ConstantValidationErrorMessage;
import com.nextgen.gameaggregator.vendor.api.pgsoft.component.dto.AbstractActionDto;

import javax.validation.constraints.*;
import java.math.BigDecimal;

public class CashTransferInOutActionDto extends AbstractActionDto {

    /**
     * Authentication Information
     */
    //* Below are not mandatory
    private String operatorPlayerSession;

    /**
     * General Bet Information
     */

    //* Below are mandatory
    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String playerName;

    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    @Positive(message = ConstantValidationErrorMessage.MUST_BE_POSITIVE)
    private Integer gameId;

    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String parentBetId;

    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String betId;

    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String currencyCode;

    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    private BigDecimal betAmount;

    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    private BigDecimal winAmount;

    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    private BigDecimal transferAmount;

    @NotBlank(message = ConstantValidationErrorMessage.CANNOT_BE_BLANK)
    private String transactionId;

    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    @Positive(message = ConstantValidationErrorMessage.MUST_BE_POSITIVE)
    private Integer betType;

    @Positive(message = ConstantValidationErrorMessage.MUST_BE_POSITIVE)
    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    private Long createTime;

    @Positive(message = ConstantValidationErrorMessage.MUST_BE_POSITIVE)
    @NotNull(message = ConstantValidationErrorMessage.CANNOT_BE_NULL)
    private Long updatedTime;

    //* Below are not mandatory
    private String walletType;
    private String platform;

    /**
     * Bet Indicator
     */
    //* Below are not mandatory
    private Boolean isValidateBet;
    private Boolean isAdjustment;
    private Boolean isParentZeroStake;
    private Boolean isFeature;
    private Boolean isFeatureBuy;
    private Boolean isWager;

    /**
     * Free Game Information
     */
    //* Below are not mandatory
    private String freeGameTransactionId;
    private String freeGameName;
    private Integer freeGameId;
    private Boolean isMinusCount;

    /**
     * Bonus Game Information
     */
    //* Below are not mandatory
    private String bonusTransactionId;
    private String bonusName;
    private Integer bonusId;
    private BigDecimal bonusBalanceAmount;
    private BigDecimal bonusRatioAmount;

}
