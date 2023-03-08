package com.nextgen.gameaggregator.vendor.facai.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

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
    public BalanceVo balance(HttpServletRequest request) throws InvalidRequestException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        BalanceVo balanceVo = new BalanceVo();

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

            //return balance and success code
            balanceVo.setResult(ResponseCodes.SUCCESS);
            balanceVo.setMainPoints(balance);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            balanceVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            balanceVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (AuthenticationException authenticationException) {
            balanceVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            balanceVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            balanceVo.setResult(ResponseCodes.PLAYER_NOT_FOUND);
            balanceVo.setErrorText(ResponseCodes.PLAYER_NOT_FOUND_MSG);
        } catch (JsonProcessingException jsonProcessingException) {
            balanceVo.setResult(ResponseCodes.UNEXPECTED_ERROR);
            balanceVo.setErrorText(ResponseCodes.UNEXPECTED_ERROR_MSG);
        } catch (Exception exception) {
            balanceVo.setResult(ResponseCodes.UNEXPECTED_ERROR);
            balanceVo.setErrorText(ResponseCodes.UNEXPECTED_ERROR_MSG);
        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;

    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

}
