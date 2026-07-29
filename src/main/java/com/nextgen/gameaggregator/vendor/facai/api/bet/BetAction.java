package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.EncryptionUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.Encryption;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class BetAction {

    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
        BetDto betDto = new BetDto();
        boolean isRequestExists = false;
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
            String secret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AGENT_KEY);
            String jsonParam = EncryptionUtils.aesDecrypt(Encryption.CIPHER_MODE_AND_PADDING, commonDto.getParams(), secret);
            httpRequestLog.setRequestBody(body + ", Decrypt Value:" + jsonParam);

            //map decrypted data(string json) into betDto
            betDto = HttpService.convertJsonToDto(jsonParam, BetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(betDto);

            if (requestIdempotentLogService.checkExists(betDto, betDto.getMemberAccount()) == null) {
                requestIdempotentLogService.create(betDto, betDto.getMemberAccount());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            //calculate vendor threshold. if the time is over 4 sec, direct send error to vendor.
            this.checkVendorTimeout(betDto);

            //get rawGameSession by player username without game id
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(betDto.getMemberAccount());
            if (gameSession == null) throw new AuthenticationException();
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getGameId(), gameSession);

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
            if (!betResultIdempotentViolationException.getStatus().equals(BetStatus.SETTLED.code)) {
                //if bet result idempotent is not settled (which is rollback, then we should let vendor resend rollback instead
                commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
                httpService.logError(httpRequestLog, betResultIdempotentViolationException);

            } else {
                commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
                commonVo.setMainPoints(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());
                httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            }

        } catch (
                AuthenticationException |
                InvalidDecryptionException |
                CredentialNotFoundException |
                DisabledVendorLineException |
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

            if (invalidOperatorResponseException.getOperatorStatus()
                    .equals(Status.SC_INSUFFICIENT_FUNDS.code)) {
                commonVo.setErrorResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);
            } else {
                commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);
            }

        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
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

        } catch (BetFailedException betFailedException) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
            httpService.logError(httpRequestLog, betFailedException);
        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
            //commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betDto, betDto.getMemberAccount());
            }
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doDecryptValidation(BetDto dto) throws InvalidRequestException, InvalidDateException {
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

    private void doVerification(CommonDto commonDto, BetDto betDto, GameSession gameSession, String jsonParam) throws
            AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException,
            CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = DigestUtils.md5Hex(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(agentCode, commonDto.getAgentCode(), InvalidRequestException::new);

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betDto.getMemberAccount());
    }

    private void checkVendorTimeout(BetDto betDto) throws BetFailedException {
        if (betDto.getTs() != null && System.currentTimeMillis() - betDto.getTs() >= 4000) {
            throw new BetFailedException("Round Id: " + betDto.getRoundId() + "(Received request is too late. The vendor threshold timeout is 4 seconds.)");
        }
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
