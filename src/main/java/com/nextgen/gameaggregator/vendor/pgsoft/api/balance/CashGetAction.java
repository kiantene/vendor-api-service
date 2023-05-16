package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;


import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestScope
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class CashGetAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.BALANCE)
    public ResponseVo<CashGetVo> balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        // Construct Vo
        ResponseVo<CashGetVo> parentResponseVo = new ResponseVo<>();
        CashGetVo responseVo = new CashGetVo();
        parentResponseVo.setData(responseVo);

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert original request body into dto
            CashGetDto dto = HttpService.convertQueryStringToDto(body, CashGetDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);
            // 2. Verify session token
            // Need to validate whether game session expired
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession gameSession = gameSessionService.verifyToken(dto.getOperatorPlayerSession());
            // 3. Validate vendor player username
            // TODO - to refactor ValidationUtil.validateEqual to throw custom exception class
            VendorService.validatePlayerUsername(gameSession.getVendorPlayerUsername(), dto.getPlayerName());
            // 4. Retrieve vendor line operatorToken and secretKey for validation
            String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
            String operatorToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_TOKEN);
            // 5. Validate request operatorToken and secretKey
            VendorService.validateOperatorTokenAndSecretKey(dto.getOperatorToken(), dto.getSecretKey(), operatorToken, secretKey);
            // 6. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            // Fill VO required values
            responseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            responseVo.setBalanceAmount(balance);
            responseVo.setUpdatedTime(Instant.now().toEpochMilli());

        } catch (InvalidRequestException invalidRequestException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (AuthenticationException authenticationException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_PLAYER_SESSION_1300);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_PLAYER_SESSION_1300));

        } catch (InvalidPlayerException invalidPlayerException) {
            parentResponseVo.setErrorCode(ResponseCodes.PLAYER_DOES_NOT_EXIST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.PLAYER_DOES_NOT_EXIST));

        } catch (CredentialNotFoundException credentialNotFoundException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (NoAvailableLineException noAvailableLineException) {
            parentResponseVo.setErrorCode(ResponseCodes.INVALID_REQUEST);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INVALID_REQUEST));

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            parentResponseVo.setErrorCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INTERNAL_SERVER_ERROR));

        } catch (Exception exception) { // any other exception encountered
            parentResponseVo.setErrorCode(ResponseCodes.INTERNAL_SERVER_ERROR);
            parentResponseVo.setErrorMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.INTERNAL_SERVER_ERROR));
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, parentResponseVo);
        }

        //
        return parentResponseVo;
    }

}