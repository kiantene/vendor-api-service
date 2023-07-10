package com.nextgen.gameaggregator.vendor.habanero.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferResponseVo;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.dto.BetDto;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;

@Service
@Slf4j
public class BetService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;

    public TransferVo bet(FundInfoDto fundInfoDto, FundTransferRequestDto fundTransferRequestDto, TransferVo transferVo, String gameId, GameSession gameSession, String traceId, String body, HttpRequestLog httpRequestLog) {
        // Construct VO
        TransferVo responseVo = transferVo;
        FundTransferResponseVo fundTransferResponseVo = transferVo.getFundTransferResponseVo();
        StatusVo statusVo = transferVo.getFundTransferResponseVo().getStatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        try {

            //construct betDto
            BetDto betDto = this.processBetData(fundInfoDto, fundTransferRequestDto, gameId);

            //process unsettle bet data
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

            //return success respond
            statusVo.setSuccess(true);
            fundTransferResponseVo.setBalance(betEvent.getLastBalance().setScale(2, RoundingMode.DOWN));
            fundTransferResponseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            if (fundTransferRequestDto.getFundDto().getDebitAndCredit()) {
                //setup debit and credit bet type respond message
                statusVo.setSuccessDebit(true);
            }

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //return success respond when bet found
            statusVo.setSuccess(true);
            fundTransferResponseVo.setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));
            fundTransferResponseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            if (fundTransferRequestDto.getFundDto().getDebitAndCredit()) {
                //setup debit and credit bet type respond message
                statusVo.setSuccessDebit(true);
            }
        } catch (InsufficientBalanceException insufficientBalanceException) {
            statusVo.setSuccess(false);
            statusVo.setNoFunds(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            //return invalid respond to trigger vendor resend when record still in processing
            statusVo.setRetryStatus(true);
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

    private BetDto processBetData(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, String gameId) {

        // Construct bet Dto
        BetDto betDto = new BetDto();
        betDto.setExternalTransactionId(dto.getTransferId());
        betDto.setVendorBetId(fundTransferRequestDto.getFriendlyGameInstanceId());
        betDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
        betDto.setGameId(gameId);
        betDto.setBetAmount(dto.getAmount().abs());
        betDto.setWinAmount(null);
        betDto.setWinLoss(null);
        betDto.setEffectiveTurnover(null);
        betDto.setRawVendorBetTime(dto.getDtEvent());
        betDto.setRawResultTime(dto.getDtEvent());
        betDto.setRawVendorSettleTime(dto.getDtEvent());
        betDto.setJackpotAmount(null);
        betDto.setIsFreespin(0);
        betDto.setBetStatus(BetStatus.UNSETTLED);

        return betDto;
    }
}
