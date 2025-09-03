package com.nextgen.gameaggregator.vendor.facai.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.EncryptionUtils;
import com.nextgen.gameaggregator.util.ValidationUtils;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@RequiredArgsConstructor
public class CancelBetAction {

    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @PostMapping(path = EndPoints.CANCEL_BET)
    public CommonVo cancelbet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        // using for check the operatorStatus of transaction through the couchbase
        SettledBet settledBet = null;
        GameSession gameSession;

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

            //map decrypted data(string json) into cancelBetDto
            CancelBetDto cancelbetDto = HttpService.convertJsonToDto(jsonParam, CancelBetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(cancelbetDto);

            //get rawGameSession by player username without game id


            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(cancelbetDto.getMemberAccount());
            if (gameSession == null) throw new AuthenticationException();
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(cancelbetDto.getGameID().toString(), gameSession);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, cancelbetDto, gameSession, jsonParam);

            BigDecimal balance = walletService.processRollback(traceId, cancelbetDto, gameSession, vendorService, httpRequestLog);

            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (RecordNotFoundException recordNotFoundException) {
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(0d);
            httpService.logError(httpRequestLog, recordNotFoundException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                commonVo.setErrorResponseCode(ResponseCodes.REVERT_CANCEL_BET);

            } else {
                //if found the bet other in settled status (cancel / refund)
                commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
                commonVo.setMainPoints(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN).doubleValue());

            }
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                //insufficient balance
                commonVo.setErrorResponseCode(ResponseCodes.REVERT_CANCEL_BET);
                commonVo.setMainPoints(0d);

                // check the previous value before go through rollback function to decide keep or cancel transaction from vendor side
                if (settledBet != null && !settledBet.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_OK.code)) {
                    commonVo.setErrorResponseCode(ResponseCodes.SUCCESS);
                    commonVo.setMainPoints(0d);
                }

            } else if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);

            } else {
                //Other operator errors
                commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);

            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (
                InvalidDecryptionException |
                InvalidPlayerException |
                InvalidRequestException |
                CurrencyNotSupportedException |
                JsonProcessingException |
                CredentialNotFoundException |
                DisabledGameException |
                InvalidAgentApiCredentialException otherException) {
            commonVo.setErrorResponseCode(ResponseCodes.TRANSACTION_NOT_EXIST);
            httpService.logError(httpRequestLog, otherException);

        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, commonVo);

        }

        return commonVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doDecryptValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto commonDto, CancelBetDto cancelbetDto, GameSession gameSession, String jsonParam) throws InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, DisabledGameException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), cancelbetDto.getMemberAccount(), InvalidPlayerException::new);

        //Verify received game id is the same from game session
        //comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), Integer.toString(cancelbetDto.getGameID()), DisabledGameException::new);

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelbetDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = DigestUtils.md5Hex(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(agentCode, commonDto.getAgentCode(), InvalidRequestException::new);

    }
}
