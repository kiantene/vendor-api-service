package com.nextgen.gameaggregator.vendor.hacksawgaming.api.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class LoginAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.BALANCE)
    public LoginVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        LoginVo loginVo = new LoginVo();
        LoginDataVo loginDataVo = new LoginDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            LoginDto dto = HttpService.convertJsonToDto(body, LoginDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(VendorService.revertToUUID(dto.getToken()));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> bodyObj = mapper.readValue(body, Map.class);
            this.doVerification(dto, gameSession, bodyObj);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Vendor player username + Balance + Currency
            loginDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            loginDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            loginDataVo.setBalance(balance);

            // Set BalanceDataWalletVo Object
            loginVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SUCCESS));
            loginVo.setCode(ResponseCodes.SUCCESS);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
        }
        httpService.end(httpRequestLog, loginVo);

        return loginVo;

    }

    private void doValidation(LoginDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(LoginDto dto, GameSession gameSession, Map<String, Object> body)
            throws AuthenticationException,
            InvalidPlayerException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException {


        // Verify received username is the same from game session
        // ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);
        if(!gameSession.getVendorPlayerUsername().equals(dto.getBrandUid())) {
            throw new InvalidPlayerException();
        }

//        String signatureKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE_KEY);
//        if(!VendorService.isSameSignature(token, body, signatureKey)) {
//            throw new InvalidVendorLineException();
//        }

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getBrandUid());
    }
}
