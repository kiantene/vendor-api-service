package com.nextgen.gameaggregator.vendor.habanero.api.result;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundInfoDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.FundTransferRequestDto;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class ResultService {

    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    public TransferVo result(FundInfoDto fundInfoDto, FundTransferRequestDto fundTransferRequestDto, TransferVo responseVo, String gameId, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) throws
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
            DisabledVendorLineException
    {

        try {

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(fundInfoDto);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(fundInfoDto, fundTransferRequestDto, gameSession);

            //construct result Dto
            ResultDto resultDto = new ModelMapper().map(fundInfoDto, ResultDto.class);
            resultDto.setVendorBetId(fundTransferRequestDto.getFriendlyGameInstanceId());
            resultDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
            resultDto.setGameId(gameId);

            ResultType resultType = this.getResultType(fundInfoDto);

            //process bet result data (settle or unsettle)
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

    private void doVerification(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, GameSession gameSession) throws
            NoAvailableLineException,
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException {

        //Verify vendor currency code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrencyCode(), NoAvailableLineException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        if(dto.getIsBonus() == true){
            //bonus free spin will be settled without bet
            validationService.validateEligibleBet(gameSession, fundTransferRequestDto.getAccountId());
        }

    }

    private ResultType getResultType(FundInfoDto dto) {

        ResultType resultType = ResultType.WIN;

        if (dto.getIsBonus() == true) {
            //bonus free spin will be settled without bet
            resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
        } else {
            //settle bet, jackpot and free spin
            resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.END;
        }

        return resultType;
    }
}
