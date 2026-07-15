package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.BonusDetailDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class HabaneroBonusPayoutProcessor {

    private final HabaneroBonusPayoutHandler habaneroBonusPayoutHandler;

    public HabaneroBonusPayoutProcessor(HabaneroBonusPayoutHandler habaneroBonusPayoutHandler) {
        this.habaneroBonusPayoutHandler = habaneroBonusPayoutHandler;
    }

    public TransferVo process(TransferDto transferDto, String traceId) throws InvalidRequestException, TransactionStillProcessingException {
        FundTransferRequestDto fundTransferRequest = transferDto.getFundTransferRequestDto();
        FundDto funds = fundTransferRequest.getFundDto();
        if (funds == null || funds.getFundInfoDto() == null || funds.getFundInfoDto().length == 0) {
            throw new InvalidRequestException("fund info is required for regular bonus payout");
        }
        FundInfoDto fundInfo = funds.getFundInfoDto()[0];

        validate(fundInfo, fundTransferRequest);

        HabaneroBonusPayoutRequest request = HabaneroBonusPayoutRequest.builder()
                .fundTransferRequest(fundTransferRequest)
                .fundInfo(fundInfo)
                .bonusDetails(fundTransferRequest.getBonusDetailDto())
                .vendorGameCode(transferDto.getBaseGame().getKeyName())
                .build();

        try {
            return habaneroBonusPayoutHandler.process(request);
        } catch (DuplicateRequestException duplicate) {
            if (duplicate.getCurrency() == null) {
                // idempotency row was written at entry but never enriched — the first attempt failed
                // after the guard check; returning success here would silently lose the payout.
                // Throwing causes TransferAction to return HTTP 503 so the vendor retries.
                throw new TransactionStillProcessingException("previous bonus payout attempt did not complete");
            }
            return buildDuplicateSuccessResponse(duplicate);
        }
    }

    private void validate(FundInfoDto fundInfo, FundTransferRequestDto fundTransferRequest) throws InvalidRequestException {
        ValidationUtils.validateRequest(fundInfo);
        if (fundInfo.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("amount cannot be negative for regular bonus payout");
        }
        BonusDetailDto bonusDetails = fundTransferRequest.getBonusDetailDto();
        if (bonusDetails == null) {
            throw new InvalidRequestException("bonus details are required for regular bonus payout");
        }
        if (bonusDetails.getBonusBalanceId() == null || bonusDetails.getBonusBalanceId().isBlank()) {
            throw new InvalidRequestException("bonusbalanceid is required for regular bonus payout");
        }
        if (bonusDetails.getCouponId() == null || bonusDetails.getCouponId().isBlank()) {
            throw new InvalidRequestException("couponid is required for regular bonus payout");
        }
    }

    private TransferVo buildDuplicateSuccessResponse(DuplicateRequestException duplicate) {
        TransferVo responseVo = new TransferVo();
        responseVo.setResponseCode(ResponseCodes.TRANSFER_SUCCESS);
        responseVo.getFundTransferResponseVo().setBalance(duplicate.getBalance().setScale(2, RoundingMode.DOWN));
        responseVo.getFundTransferResponseVo().setCurrencyCode(duplicate.getCurrency());
        return responseVo;
    }
}
