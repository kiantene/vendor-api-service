package com.nextgen.gameaggregator.vendor.habanero.api.pokerresult;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class PokerResultService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    public TransferVo result(FundInfoDto fundInfoDto, FundTransferRequestDto fundTransferRequestDto, TransferVo responseVo, String gameId, GameSession gameSession, HttpServletRequest request) throws
            InvalidAgentApiCredentialException,
            BetNotFoundException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            VendorCurrencyNotSupportException,
            InvalidRequestException,
            NoAvailableLineException,
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException {

        //Regenerate new trace ID, Set Request Body
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        httpRequestLog.setRequestBody(new Gson().toJson(fundInfoDto));

        try {
            //Validate request parameters from vendor (Non-database related)
            this.doValidation(fundInfoDto);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(fundInfoDto, fundTransferRequestDto, gameSession, responseVo);

            //construct result Dto
            PokerResultDto resultDto = new ModelMapper().map(fundInfoDto, PokerResultDto.class);
            resultDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
            resultDto.setGameId(gameId);

            //process bet result data (settle or unsettle)
            ResultType resultType = vendorService.calculateResultType(resultDto.getBetAmount(), resultDto.getWinAmount(), resultDto.getJackpotAmount(), true);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, resultDto, resultType, vendorService, httpRequestLog);

            //return success respond
            responseVo.setResponseCode(ResponseCodes.TRANSFER_SUCCESS);
            responseVo.getFundTransferResponseVo().setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.getFundTransferResponseVo().setCurrencyCode(gameSession.getVendorCurrencyCode());
            if (fundTransferRequestDto.getFundDto().getDebitAndCredit()) {
                //setup debit and credit bet type respond message
                responseVo.getFundTransferResponseVo().getStatusVo().setSuccessCredit(true);
            }

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //return success respond when bet found
            responseVo.setResponseCode(ResponseCodes.TRANSFER_SUCCESS);
            responseVo.getFundTransferResponseVo().setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));
            responseVo.getFundTransferResponseVo().setCurrencyCode(gameSession.getVendorCurrencyCode());
            if (fundTransferRequestDto.getFundDto().getDebitAndCredit()) {
                //setup debit and credit bet type respond message
                responseVo.getFundTransferResponseVo().getStatusVo().setSuccessCredit(true);
            }
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCodes.TRANSFER_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(FundInfoDto dto) throws InvalidRequestException {

        // General validation
        ValidationUtils.validateRequest(dto);

        //date time format validation
        if (!vendorService.isValidDateString(dto.getDtEvent())) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, GameSession gameSession, TransferVo responseVo) throws
            NoAvailableLineException, InvalidRequestException {

        //Verify vendor currency code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrencyCode(), NoAvailableLineException::new);

        //check if it's a settle-only or a bet & settle
        if (fundTransferRequestDto.getFundDto().getDebitAndCredit() && !responseVo.getFundTransferResponseVo().getStatusVo().getSuccessDebit()) {
            throw new InvalidRequestException();
        }
    }

}
