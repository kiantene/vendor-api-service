package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferDto;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HabaneroRegularBonusSupportTest {

    private TransferDto validTransferDto() {
        FundInfoDto fundInfo = new FundInfoDto();
        fundInfo.setIsBonus(true);
        fundInfo.setGameStateMode(GameStateMode.ENDROUND);
        fundInfo.setAmount(BigDecimal.TEN);
        fundInfo.setInitialDebitTransferId(null);

        FundDto fundDto = new FundDto();
        fundDto.setDebitAndCredit(false);
        fundDto.setFundInfoDto(new FundInfoDto[]{fundInfo});

        FundTransferRequestDto fundTransferRequest = new FundTransferRequestDto();
        fundTransferRequest.setIsRefund(false);
        fundTransferRequest.setBonusDetailDto(new BonusDetailDto());
        fundTransferRequest.setFundDto(fundDto);

        TransferDto dto = new TransferDto();
        dto.setFundTransferRequestDto(fundTransferRequest);
        return dto;
    }

    @Test
    void isRegularBonusPayout_returnsTrue_whenAllConditionsMet() {
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(validTransferDto())).isTrue();
    }

    @Test
    void isRegularBonusPayout_returnsTrue_whenAmountIsZero() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setAmount(BigDecimal.ZERO);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isTrue();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenTransferDtoIsNull() {
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(null)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenFundTransferRequestIsNull() {
        TransferDto dto = new TransferDto();
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenIsRefundTrue() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().setIsRefund(true);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenBonusDetailsNull() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().setBonusDetailDto(null);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenFundDtoIsNull() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().setFundDto(null);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenFundInfoIsNull() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().getFundDto().setFundInfoDto(null);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenFundInfoLengthIsNotOne() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().getFundDto().setFundInfoDto(new FundInfoDto[]{new FundInfoDto(), new FundInfoDto()});
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenDebitAndCreditTrue() {
        TransferDto dto = validTransferDto();
        dto.getFundTransferRequestDto().getFundDto().setDebitAndCredit(true);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenIsBonusFalse() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setIsBonus(false);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenGameStateModeNotEndRound() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setGameStateMode(1);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenInitialDebitTransferIdPresent() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setInitialDebitTransferId("debit-001");
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenAmountIsNull() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setAmount(null);
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    @Test
    void isRegularBonusPayout_returnsFalse_whenAmountIsNegative() {
        TransferDto dto = validTransferDto();
        fundInfo(dto).setAmount(new BigDecimal("-0.01"));
        assertThat(HabaneroRegularBonusSupport.isRegularBonusPayout(dto)).isFalse();
    }

    private FundInfoDto fundInfo(TransferDto dto) {
        return dto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0];
    }
}
