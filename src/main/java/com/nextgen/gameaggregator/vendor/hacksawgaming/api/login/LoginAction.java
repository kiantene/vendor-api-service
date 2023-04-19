package com.nextgen.gameaggregator.vendor.hacksawgaming.api.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.hacksawgaming.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
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

    @PostMapping(path = EndPoints.LOGIN)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            LoginDto dto = HttpService.convertJsonToDto(body, LoginDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(VendorService.revertToUUID(dto.getToken()));

            // Verify data
            this.doVerification(dto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);

            // Set BalanceDataWalletVo Object
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SUCCESS));
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

        } catch(InvalidRequestException |
                DateTimeParseException |
                CurrencyNotSupportedException |
                JsonProcessingException |
                NullPointerException |
                IllegalArgumentException e
        ) {
            responseVo.setCode(ResponseCodes.SYSTEM_ERROR);
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.SYSTEM_ERROR));
            responseVo.setData(null);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setCode(ResponseCodes.UNKNOWN);
            responseVo.setMsg(ResponseCodes.RESPONSE_DESCRIPTION.get(ResponseCodes.UNKNOWN));
            responseVo.setData(null);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(LoginDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(LoginDto dto, GameSession gameSession)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getToken() + apiKey);

        // Verify signature
        if(!VendorService.isSameSignature(dto.getSign(), toVerifySign)) {
            throw new InvalidVendorLineException();
        }

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getBrandUid());

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
