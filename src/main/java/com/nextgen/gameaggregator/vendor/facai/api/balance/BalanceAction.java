package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {


    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private GameSessionService gameSessionService;

    @PostMapping(path = EndPoints.BALANCE)
    public CommonVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //TODO pending PG update core function to get appKey
            //Decrypt raw respond
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");
            log.info("json: " + jsonParam);
            //map decrypted data(string json) into balanceDto
            BalanceDto balanceDto = HttpService.convertJsonToDto(jsonParam, BalanceDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(balanceDto);

            //get gameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(balanceDto.getMemberAccount(), Integer.toString(balanceDto.getGameID()));

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, balanceDto, gameSession, jsonParam);

            //return double balance and success code
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (InvalidRequestException invalidRequestException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.CURRENCY_MISSING);
        } catch (InvalidPlayerException invalidPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PLAYER_NOT_FOUND);
        } catch (DisabledGameException disabledGameException) {
            commonVo.setErrorResponseCode(ResponseCodes.GAME_NOT_FOUND);
        } catch (InvalidDecryptionException invalidDecryptionException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (CredentialNotFoundException credentialNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (AuthenticationException authenticationException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (JsonProcessingException jsonProcessingException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getAgentCode())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getSign())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
    }

    private void doDecryptValidation(BalanceDto dto) throws InvalidRequestException, InvalidPlayerException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getMemberAccount())) {throw new InvalidPlayerException();}
        if(!vendorService.isValidStringLength(dto.getMemberAccount(), 2, 30)) {throw new InvalidPlayerException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidInteger(dto.getGameID())) {throw new InvalidRequestException();}
        if(dto.getTs() == null || !vendorService.isValidTimestamp(dto.getTs())) {throw new InvalidRequestException();}
    }

    private void doVerification(CommonDto commonDto, BalanceDto balanceDto, GameSession gameSession, String jsonParam) throws AuthenticationException, InvalidRequestException, InvalidPlayerException, DisabledGameException, CurrencyNotSupportedException, CredentialNotFoundException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), balanceDto.getMemberAccount(), InvalidPlayerException::new);

        //Verify received game id is the same from game session
        //comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), Integer.toString(balanceDto.getGameID()), DisabledGameException::new);

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), balanceDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = vendorService.md5(jsonParam);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidRequestException();
        }
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);


        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(AgentCode, commonDto.getAgentCode(), InvalidRequestException::new);


    }

}
