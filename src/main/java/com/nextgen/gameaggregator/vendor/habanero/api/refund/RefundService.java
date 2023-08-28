package com.nextgen.gameaggregator.vendor.habanero.api.refund;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.RefundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RefundService {

    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    public TransferVo refund(RefundDto refundDto, TransferVo responseVo, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) throws
            InvalidAgentApiCredentialException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            BetResultIdempotentViolationException,
            VendorCurrencyNotSupportException,
            InvalidRequestException,
            NoAvailableLineException
    {

        try {

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(refundDto);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(refundDto, gameSession);

            //handle when unsettle bet available, refund and void the game
            BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService);

            //void the game
            responseVo.setResponseCode(ResponseCodes.REFUNDED);

        } catch (
                RecordNotFoundException |
                BetNotFoundException betNotFoundException
        ) {
            //handle when unsettle bet not available, no action and void the game
            responseVo.setResponseCode(ResponseCodes.REFUND_NOT_REQUIRE);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            //void the game
            responseVo.setResponseCode(ResponseCodes.REFUNDED);

        }

        return responseVo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException {

        // General validation
        ValidationUtils.validateRequest(dto);

        //date time format validation
        if (!vendorService.isValidDateString(dto.getDtEvent())) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(RefundDto dto, GameSession gameSession) throws NoAvailableLineException{

        //Verify vendor currency code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrencyCode(), NoAvailableLineException::new);
    }

}
