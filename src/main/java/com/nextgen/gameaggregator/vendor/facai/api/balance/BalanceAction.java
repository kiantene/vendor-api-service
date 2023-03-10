package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
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

            //map decrypted data(string json) into balanceDto
            BalanceDto balanceDto = HttpService.convertJsonToDto(jsonParam, BalanceDto.class);

            //Get vendor player details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getMemberAccount());

            //Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            //return double balance and success code
            commonVo.setResult(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(balance.setScale(2, RoundingMode.DOWN).doubleValue());

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            commonVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            commonVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (AuthenticationException authenticationException) {
            commonVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            commonVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            commonVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            commonVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (JsonProcessingException jsonProcessingException) {
            commonVo.setResult(ResponseCodes.UNEXPECTED_ERROR);
            commonVo.setErrorText(ResponseCodes.UNEXPECTED_ERROR_MSG);
        } catch (Exception exception) {
            commonVo.setResult(ResponseCodes.UNEXPECTED_ERROR);
            commonVo.setErrorText(ResponseCodes.UNEXPECTED_ERROR_MSG);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto balanceDto, GameSession gameSession) throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), balanceDto.getMemberAccount(), InvalidPlayerException::new);


    }

}
