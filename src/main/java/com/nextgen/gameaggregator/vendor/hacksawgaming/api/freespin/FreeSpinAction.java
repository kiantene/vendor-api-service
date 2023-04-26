package com.nextgen.gameaggregator.vendor.hacksawgaming.api.freespin;

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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class FreeSpinAction {

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

    @PostMapping(path = EndPoints.FREE_SPIN_RESULT)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            /*
            TODO: This endpoint will only be triggered if Free Spin Campaign is set up.
             To update this endpoint if Free Spin Campaign is required to set up.
             This endpoint only return current balance for now
             */

            FreeSpinDto dto = HttpService.convertJsonToDto(body, FreeSpinDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getBrandUid(), dto.getGameId().toString());

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

        } catch(InvalidRequestException |
                CurrencyNotSupportedException |
                NullPointerException |
                IllegalArgumentException e
        ) {

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
        }
        httpService.end(httpRequestLog, responseVo);

        return responseVo;

    }

    private void doValidation(FreeSpinDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(FreeSpinDto dto, GameSession gameSession)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException, AuthenticationException {

            String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
            String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
            String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

            // Verify signature
            if(!VendorService.isSameSignature(dto.getSign(), toVerifySign)) {
                throw new InvalidVendorLineException();
            }

            // validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, dto.getBrandUid());

            // Verify currency
            ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
