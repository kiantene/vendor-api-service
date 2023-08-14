package com.nextgen.gameaggregator.vendor.booongo.api.freespin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.booongo.service.VendorService;
import com.nextgen.gameaggregator.vendor.booongo.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.booongo.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

@Service
@Slf4j
public class FreeSpinService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    public CommonVo freespin(HttpRequestLog httpRequestLog, String traceId){
        // Construct VO
        BalanceVo balanceVo = new BalanceVo();
        FreeSpinVo vo = new FreeSpinVo();
        ErrorVo errorVo = new ErrorVo();
        FreeSpinDto freeSpinDto = new FreeSpinDto();

        BigDecimal balance = null;
        long unixTime = System.currentTimeMillis(); //unix timestamp with millisecond
        GameSession gameSession = new GameSession();

        try{
            // Retrieve request body in original string format
            freeSpinDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), FreeSpinDto.class);

            // Verify session token
            gameSession = gameSessionService.verifyToken(freeSpinDto.getToken());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(freeSpinDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(freeSpinDto, gameSession);

            // Always in BET_WIN status for free bet game
            ResultType resultType = getResultType();

            balance = walletService.processBetResult(traceId, gameSession, freeSpinDto, resultType, vendorService, httpRequestLog);

            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());

        }catch (InsufficientBalanceException e) {
            errorVo.setCode(ResponseCodes.FUNDS_EXCEED);

            balance = getCurrentBalance(traceId, gameSession);

            // Retrieve current wallet balance
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            vo.setError(errorVo);
        }catch (BetResultIdempotentViolationException e) {
            // this exception happened when handle repeated data
            balance = getCurrentBalance(traceId, gameSession);

            // Retrieve current wallet balance
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
        }catch(InvalidOperatorResponseException e){
            errorVo.setCode(ResponseCodes.SESSION_CLOSED_TRANSACTION);

            // check the status is insufficient code or not
            if(e.getOperatorStatus() == com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code){
                errorVo.setCode(ResponseCodes.FUNDS_EXCEED);
            }

            balance = getCurrentBalance(traceId, gameSession);

            // Retrieve current wallet balance
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            vo.setError(errorVo);
        }catch (DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                InvalidPlayerException |
                CurrencyNotSupportedException |
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidRequestException |
                BetNotFoundException |
                GameNotSupportedException |
                JsonProcessingException |
                AuthenticationException |
                TransactionStillProcessingException |
                CredentialNotFoundException e) {
            errorVo.setCode(ResponseCodes.SESSION_CLOSED_TRANSACTION);

            balance = getCurrentBalance(traceId, gameSession);

            // Retrieve current wallet balance
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            vo.setError(errorVo);
        }catch(Exception exception){
            httpService.logError(httpRequestLog, exception);
            errorVo.setCode(ResponseCodes.SESSION_CLOSED_TRANSACTION);

            balance = getCurrentBalance(traceId, gameSession);

            // Retrieve current wallet balance
            balanceVo.setValue(balance.setScale(2, RoundingMode.DOWN).toString());
            vo.setError(errorVo);
        }finally{
            balanceVo.setVersion(BigInteger.valueOf(unixTime));
            vo.setUid(freeSpinDto.getUid());
            vo.setBalance(balanceVo);
        }

        return vo;
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession) {

        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession);

        } catch (Exception exception) {

        }

        return balance;
    }

    private void doValidation(FreeSpinDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getArgs());

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getArgs().getPlayer());
    }

    private void doVerification(FreeSpinDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidRequestException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getArgs().getPlayer().getId());

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGame_id(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getArgs().getPlayer().getCurrency(), CurrencyNotSupportedException::new);

        //Verify received brand is same with credential
        String brand = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROJECT_NAME);
        ValidationUtils.isEquals(brand, dto.getArgs().getPlayer().getBrand(), InvalidRequestException::new);
    }

    private ResultType getResultType(){
        // free bet game always in win status
        ResultType resultType = ResultType.BET_WIN; // Default value is win

        return resultType;
    }
}
