package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferDto;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;

import java.math.BigDecimal;

/**
 * Identifies Habanero Regular Bonus payout callbacks (free-round completion credit via fundtransferrequest).
 */
public final class HabaneroRegularBonusSupport {

    private HabaneroRegularBonusSupport() {
    }

    public static boolean isRegularBonusPayout(TransferDto transferDto) {
        if (transferDto == null || transferDto.getFundTransferRequestDto() == null) {
            return false;
        }
        FundTransferRequestDto request = transferDto.getFundTransferRequestDto();
        if (Boolean.TRUE.equals(request.getIsRefund())) {
            return false;
        }
        if (request.getBonusDetailDto() == null) {
            return false;
        }
        FundDto funds = request.getFundDto();
        if (funds == null || funds.getFundInfoDto() == null || funds.getFundInfoDto().length != 1) {
            return false;
        }
        if (Boolean.TRUE.equals(funds.getDebitAndCredit())) {
            return false;
        }
        FundInfoDto fundInfo = funds.getFundInfoDto()[0];
        if (!Boolean.TRUE.equals(fundInfo.getIsBonus())) {
            return false;
        }
        if (!Integer.valueOf(GameStateMode.ENDROUND).equals(fundInfo.getGameStateMode())) {
            return false;
        }
        if (hasInitialDebitTransferId(fundInfo)) {
            return false;
        }
        return fundInfo.getAmount() != null && fundInfo.getAmount().compareTo(BigDecimal.ZERO) >= 0;
    }

    private static boolean hasInitialDebitTransferId(FundInfoDto fundInfo) {
        String initialDebitTransferId = fundInfo.getInitialDebitTransferId();
        return initialDebitTransferId != null && !initialDebitTransferId.isBlank();
    }
}
