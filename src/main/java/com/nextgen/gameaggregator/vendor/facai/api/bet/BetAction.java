package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        BetDto betDto = null;

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //Get vendor line id by agent code from vendor line credential
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT_CODE, commonDto.getAgentCode());

            //Decrypt raw respond with key from vendor line credential
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AGENT_KEY), httpRequestLog, body);

            //map decrypted data(string json) into betDto
            betDto = HttpService.convertJsonToDto(jsonParam, BetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(betDto);

            //get rawGameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getMemberAccount(), betDto.getGameId());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, betDto, gameSession, jsonParam);

            //Process full bet data
            ResultType resultType = this.getResultType(betDto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

            //set VO data
            //convert bigDecimal balance into double
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            //commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (
                AuthenticationException |
                InvalidDecryptionException |
                InvalidEncryptionException |
                CredentialNotFoundException |
                DisabledVendorLineException |
                InvalidVendorLineException |
                DisabledAgentPlayerException |
                JsonProcessingException paramException
        ) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
            httpService.logError(httpRequestLog, paramException);

        } catch (
                MergedBetDataIntegrityException |
                InvalidAgentApiCredentialException |
                BetNotFoundException |
                TransactionStillProcessingException cancelException
        ) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
            httpService.logError(httpRequestLog, cancelException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {

                commonVo.setErrorResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);

                // 1 mean fish game(vendor does not allow fish game to cancel bet request, so return error to force cancel)
//                if(betDto.getGameType().equals(1)){
//                    commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
//                }else{
//
//                }
            } else {
                commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setErrorResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.CURRENCY_MISSING);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidPlayerException invalidPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (InvalidDateException invalidDateException) {
            commonVo.setErrorResponseCode(ResponseCodes.DATE_INPUT_MISSING);
            httpService.logError(httpRequestLog, invalidDateException);

        } catch (DisabledGameException disabledGameException) {
            commonVo.setErrorResponseCode(ResponseCodes.GAME_NOT_FOUND);
            httpService.logError(httpRequestLog, disabledGameException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                commonVo.setErrorResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.PARAM_CONTAIN_ERROR));
            } else {
                commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
            //commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException, CurrencyNotSupportedException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doDecryptValidation(BetDto dto) throws InvalidRequestException, InvalidPlayerException, InvalidDateException, CurrencyNotSupportedException {
        // General validation
        ValidationUtils.validateRequest(dto);
        //date format validation
        if (!vendorService.isValidDateString(dto.getGameDate(), "yyyy-MM-dd HH:mm:ss")) {
            throw new InvalidDateException();
        }
        if (!vendorService.isValidDateString(dto.getCreateDate(), "yyyy-MM-dd HH:mm:ss")) {
            throw new InvalidDateException();
        }

    }

    private void doVerification(CommonDto commonDto, BetDto betDto, GameSession gameSession, String jsonParam) throws AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidEncryptionException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = VendorService.md5(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(AgentCode, commonDto.getAgentCode(), InvalidRequestException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betDto.getMemberAccount());
    }

    private ResultType getResultType(BetDto betDto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal winAmount = betDto.getWinAmount();
        BigDecimal jackpotAmount = Optional.ofNullable(betDto.getJackpotAmount()).orElse(BigDecimal.ZERO);

        if (winAmount.compareTo(BigDecimal.ZERO) > 0 || jackpotAmount.compareTo(BigDecimal.ZERO) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }
}
