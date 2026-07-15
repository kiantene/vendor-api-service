package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;
import com.nextgen.gameaggregator.vendor.habanero.dto.BaseGameDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabaneroBonusPayoutProcessorTest {

    @Mock private HabaneroBonusPayoutHandler habaneroBonusPayoutHandler;

    @InjectMocks
    private HabaneroBonusPayoutProcessor processor;

    @Test
    void process_success_returnsHandlerResponse() throws InvalidRequestException, TransactionStillProcessingException {
        TransferDto transferDto = validTransferDto(new BigDecimal("20.00"), "bonus-bal-001", "coupon-001");
        TransferVo expected = new TransferVo();
        when(habaneroBonusPayoutHandler.process(any())).thenReturn(expected);

        TransferVo result = processor.process(transferDto, "trace-001");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void process_duplicateRequest_enriched_returnsDuplicateSuccessResponse() throws InvalidRequestException, TransactionStillProcessingException {
        TransferDto transferDto = validTransferDto(new BigDecimal("10.00"), "bonus-bal-001", "coupon-001");

        RequestIdempotentLog enrichedLog = new RequestIdempotentLog();
        enrichedLog.setCurrency("USD");
        enrichedLog.setBalance(new BigDecimal("100.00"));
        DuplicateRequestException duplicate = new DuplicateRequestException("duplicate", enrichedLog);

        when(habaneroBonusPayoutHandler.process(any())).thenThrow(duplicate);

        TransferVo result = processor.process(transferDto, "trace-001");

        assertThat(result.getFundTransferResponseVo().getStatusVo().getSuccess()).isTrue();
        assertThat(result.getFundTransferResponseVo().getBalance()).isEqualByComparingTo("100.00");
        assertThat(result.getFundTransferResponseVo().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void process_duplicateRequest_unenriched_throwsTransactionStillProcessing() throws InvalidRequestException {
        TransferDto transferDto = validTransferDto(new BigDecimal("10.00"), "bonus-bal-001", "coupon-001");

        // getCurrency() returns null when no transaction/log is set — first attempt failed before enrichment
        DuplicateRequestException unenriched = new DuplicateRequestException("unenriched duplicate");

        when(habaneroBonusPayoutHandler.process(any())).thenThrow(unenriched);

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(TransactionStillProcessingException.class)
                .hasMessageContaining("did not complete");
    }

    @Test
    void process_negativeAmount_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(new BigDecimal("-1.00"), "bonus-bal-001", "coupon-001");

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("amount cannot be negative");
    }

    @Test
    void process_nullBonusBalanceId_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(BigDecimal.TEN, null, "coupon-001");

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("bonusbalanceid is required");
    }

    @Test
    void process_blankBonusBalanceId_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(BigDecimal.TEN, "   ", "coupon-001");

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("bonusbalanceid is required");
    }

    @Test
    void process_nullCouponId_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(BigDecimal.TEN, "bonus-bal-001", null);

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("couponid is required");
    }

    @Test
    void process_nullBonusDetails_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(BigDecimal.TEN, "bonus-bal-001", "coupon-001");
        transferDto.getFundTransferRequestDto().setBonusDetailDto(null);

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("bonus details are required");
    }

    @Test
    void process_nullFundDto_throwsInvalidRequestException() {
        TransferDto transferDto = validTransferDto(BigDecimal.TEN, "bonus-bal-001", "coupon-001");
        transferDto.getFundTransferRequestDto().setFundDto(null);

        assertThatThrownBy(() -> processor.process(transferDto, "trace-001"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("fund info is required");
    }

    private TransferDto validTransferDto(BigDecimal amount, String bonusBalanceId, String couponId) {
        FundInfoDto fundInfo = new FundInfoDto();
        fundInfo.setTransferId("txn-001");
        fundInfo.setCurrencyCode("USD");
        fundInfo.setAmount(amount);
        fundInfo.setGameStateMode(GameStateMode.ENDROUND);
        fundInfo.setJpWin(false);
        fundInfo.setIsBonus(true);
        fundInfo.setDtEvent("2026-07-07T10:00:00");

        BonusDetailDto bonusDetails = new BonusDetailDto();
        bonusDetails.setBonusBalanceId(bonusBalanceId);
        bonusDetails.setCouponId(couponId);

        FundDto fundDto = new FundDto();
        fundDto.setDebitAndCredit(false);
        fundDto.setFundInfoDto(new FundInfoDto[]{fundInfo});

        FundTransferRequestDto fundTransferRequest = new FundTransferRequestDto();
        fundTransferRequest.setAccountId("player01");
        fundTransferRequest.setToken("token-xyz");
        fundTransferRequest.setIsRefund(false);
        fundTransferRequest.setFundDto(fundDto);
        fundTransferRequest.setBonusDetailDto(bonusDetails);

        BaseGameDto baseGame = new BaseGameDto();
        baseGame.setKeyName("game-001");

        TransferDto transferDto = new TransferDto();
        transferDto.setFundTransferRequestDto(fundTransferRequest);
        transferDto.setBaseGame(baseGame);
        return transferDto;
    }
}
