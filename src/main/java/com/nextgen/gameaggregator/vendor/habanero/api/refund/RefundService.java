package com.nextgen.gameaggregator.vendor.habanero.api.refund;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferResponseVo;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.RefundDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RefundService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    public TransferVo refund(RefundDto refundDto, TransferVo transferVo, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        TransferVo responseVo = transferVo;
        FundTransferResponseVo fundTransferResponseVo = transferVo.getFundTransferResponseVo();
        StatusVo statusVo = transferVo.getFundTransferResponseVo().getStatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        try {

            //handle when unsettle bet available, refund and void the game
            BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService);

            //void the game
            statusVo.setRefundStatus(1);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
        } catch (
                RecordNotFoundException |
                 BetNotFoundException betNotFoundException
        ) {
            //handle when unsettle bet not available, no action and void the game
            statusVo.setRefundStatus(2);
        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            //void the game
            statusVo.setRefundStatus(1);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;
    }

}
