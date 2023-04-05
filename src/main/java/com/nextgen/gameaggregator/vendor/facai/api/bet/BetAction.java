package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.RoundingMode;

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
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.BET)
    public CommonVo bet(HttpServletRequest request) {
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

            //Get vendor line id by agent code from vendor line credential
            Integer vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.AGENT_CODE, commonDto.getAgentCode());

            //Decrypt raw respond with key from vendor line credential
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), vendorLineService.getCredentialValueByName(vendorLineId, Credentials.AGENT_KEY));

            //map decrypted data(string json) into betDto
            BetDto betDto = HttpService.convertJsonToDto(jsonParam, BetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(betDto);

            //get gameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getMemberAccount(), betDto.getGameId());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, betDto, gameSession, jsonParam);

            //Process full bet data
            SettledBetEvent settledBetEvent = walletService.processUnsettleResultSettle(traceId, gameSession, betDto, body);

            //set VO data
            //convert bigDecimal balance into double
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(settledBetEvent.getLastBalance().setScale(2,RoundingMode.DOWN).doubleValue());

        } catch (AuthenticationException authenticationException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidDecryptionException invalidDecryptionException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidEncryptionException invalidEncryptionException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.CURRENCY_MISSING);
        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (CredentialNotFoundException credentialNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (InvalidPlayerException invalidPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PLAYER_NOT_FOUND);
        } catch (InvalidDateException invalidDateException) {
            commonVo.setErrorResponseCode(ResponseCodes.DATE_INPUT_MISSING);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledGameException disabledGameException) {
            commonVo.setErrorResponseCode(ResponseCodes.GAME_NOT_FOUND);
        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if(invalidRequestException.getValidation() != null) {
                commonVo.setErrorResponseCode(invalidRequestException.getValidation().values().stream().findFirst().orElse(ResponseCodes.PARAM_CONTAIN_ERROR));
            }else{
                commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
            }
        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
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
        ValidationUtils.validateRequest(dto);
    }

    private void doDecryptValidation(BetDto dto) throws InvalidRequestException, InvalidPlayerException, InvalidDateException, CurrencyNotSupportedException {
        // General validation
        ValidationUtils.validateRequest(dto);
        //date format validation
        if(!vendorService.isValidDateString(dto.getGameDate(), "yyyy-MM-dd HH:mm:ss")) {throw new InvalidDateException();}
        if(!vendorService.isValidDateString(dto.getCreateDate(), "yyyy-MM-dd HH:mm:ss")) {throw new InvalidDateException();}

    }

    private void doVerification(CommonDto commonDto, BetDto betDto, GameSession gameSession, String jsonParam) throws AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidEncryptionException {

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = vendorService.md5(jsonParam);
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(AgentCode, commonDto.getAgentCode(), InvalidRequestException::new);

        //Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }


}
