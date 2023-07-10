package com.nextgen.gameaggregator.vendor.habanero.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferResponseVo;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.dto.BetDto;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class ResultService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    public TransferVo result(FundInfoDto fundInfoDto, FundTransferRequestDto fundTransferRequestDto, TransferVo transferVo, String gameId, Integer type, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        TransferVo responseVo = transferVo;
        FundTransferResponseVo fundTransferResponseVo = transferVo.getFundTransferResponseVo();
        StatusVo statusVo = transferVo.getFundTransferResponseVo().getStatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        try {
            //construct betDto
            BetDto betDto = this.processBonusAndSettleData(fundInfoDto, fundTransferRequestDto, gameId, type);

            ResultType resultType = this.getResultType(fundInfoDto, type);

            //process bet result data (settle or unsettle)
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

            //return success respond
            statusVo.setSuccess(true);
            fundTransferResponseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            fundTransferResponseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            if (fundTransferRequestDto.getFundDto().getDebitAndCredit()) {
                //setup debit and credit bet type respond message
                statusVo.setSuccessCredit(true);
            }

        } catch (
                InvalidAgentApiCredentialException |
                 BetNotFoundException generalException
        ) {
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
                statusVo.setSuccessCredit(true);
            }
        } catch (TransactionStillProcessingException e) {
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

    private BetDto processBonusAndSettleData(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, String gameId, Integer type) {

        // Construct bet Dto
        BetDto betDto = new BetDto();
        betDto.setExternalTransactionId(dto.getTransferId());
        betDto.setVendorBetId(fundTransferRequestDto.getFriendlyGameInstanceId());
        betDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
        betDto.setGameId(gameId);
        betDto.setBetAmount(null);
        betDto.setWinLoss(null);
        betDto.setEffectiveTurnover(null);
        betDto.setRawVendorBetTime(null);
        betDto.setRawResultTime(dto.getDtEvent());
        betDto.setRawVendorSettleTime(dto.getDtEvent());

        if (type == GameStateMode.CREDIT_ENDROUND || type == GameStateMode.EXPIRE) {
            //handle settle bet and bonus free spin
            betDto.setWinAmount(dto.getAmount().abs());
            betDto.setJackpotAmount(null);
            betDto.setBetStatus(BetStatus.SETTLED);
            if (dto.getIsBonus() == true) {
                //bonus free spin will be settled without bet
                betDto.setBetAmount(BigDecimal.valueOf(0));
                betDto.setRawVendorBetTime(dto.getDtEvent());
                betDto.setIsFreespin(1);
            } else {
                //settle bet will settle with initial bet betId
                betDto.setIsFreespin(0);
            }
        } else {
            //type = 0, handle jackpot and free spin
            betDto.setBetStatus(BetStatus.UNSETTLED);
            if (dto.getJpWin()) {
                //if JPwin = TRUE, handle jackpot
                betDto.setWinAmount(BigDecimal.valueOf(0));
                betDto.setJackpotAmount(dto.getAmount().abs());
                betDto.setIsFreespin(0);
            } else {
                //handle free spin
                betDto.setWinAmount(dto.getAmount().abs());
                betDto.setJackpotAmount(null);
                betDto.setIsFreespin(1);
            }
        }

        return betDto;
    }

    private ResultType getResultType(FundInfoDto dto, Integer type) {

        ResultType resultType = ResultType.WIN;
        if (type == GameStateMode.CREDIT_ENDROUND || type == GameStateMode.EXPIRE) {
            //handle settle bet and bonus free spin
            if (dto.getIsBonus() == true) {
                //bonus free spin will be settled without bet
                resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
            } else {
                //settle bet will settle with initial bet betId
                resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.END;
            }
        } else {
            //type = 0, handle jackpot and free spin
            resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;
        }

        return resultType;
    }
}
