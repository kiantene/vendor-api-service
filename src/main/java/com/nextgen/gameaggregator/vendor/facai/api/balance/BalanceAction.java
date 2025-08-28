package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
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
public class BalanceAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final GameSessionService gameSessionService;

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
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

            //map decrypted data(string json) into balanceDto
            BalanceDto balanceDto = HttpService.convertJsonToDto(jsonParam, BalanceDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(balanceDto);

            //get rawGameSession by player username without game id
            gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(balanceDto.getMemberAccount());
            if (gameSession == null) throw new AuthenticationException();

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, balanceDto, gameSession, jsonParam);

            //return double balance and success code
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (
                InvalidDecryptionException |
                CredentialNotFoundException |
                InvalidAgentApiCredentialException |
                AuthenticationException |
                InvalidOperatorResponseException |
                JsonProcessingException paramException
        ) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
            httpService.logError(httpRequestLog, paramException);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.CURRENCY_MISSING);
            httpService.logError(httpRequestLog, currencyNotSupportedException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                commonVo.setErrorResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.PARAM_CONTAIN_ERROR));
            } else {
                commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
            }
            httpService.logError(httpRequestLog, invalidRequestException);

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

    private void doDecryptValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto commonDto, BalanceDto balanceDto, GameSession gameSession, String jsonParam) throws InvalidRequestException, CurrencyNotSupportedException, CredentialNotFoundException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), balanceDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = DigestUtils.md5Hex(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(agentCode, commonDto.getAgentCode(), InvalidRequestException::new);
    }
}
