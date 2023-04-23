package com.nextgen.gameaggregator.vendor.hacksawgaming.api.appendwager;

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
public class AppendWagerAction {

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

    @PostMapping(path = EndPoints.APPEND_WAGER)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        ResponseDataVo responseDataVo = new ResponseDataVo();

        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            /*
            TODO: Hacksaw does not support Jackpot.
             To update this endpoint if there are other game that supports Jackpot.
             This endpoint only return current balance for now.
             */
            AppendWagerDto dto = HttpService.convertJsonToDto(body, AppendWagerDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getBrandUid(), dto.getGameId().toString());

            this.doVerification(dto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // Set Vendor player username + Balance + Currency
            responseDataVo.setBrandUid(gameSession.getVendorPlayerUsername());
            responseDataVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseDataVo.setBalance(balance);


            // Set BalanceDataWalletVo Object
            responseVo.setCode(ResponseCodes.SUCCESS);
            responseVo.setData(responseDataVo);

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

    private void doValidation(AppendWagerDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AppendWagerDto dto, GameSession gameSession)
            throws InvalidPlayerException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidVendorLineException,
            CredentialNotFoundException {

        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        String toVerifySign = VendorService.getSign(brandId + dto.getWagerId() + apiKey);

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
