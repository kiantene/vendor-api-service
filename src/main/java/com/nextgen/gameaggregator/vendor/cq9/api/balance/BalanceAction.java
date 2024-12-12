package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceAction {
    private final GameService gameService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorPlayerService vendorPlayerService;
    private final CurrencyService currencyService;
    private final WalletService walletService;

    public BalanceAction(GameServiceImpl gameService,
                         HttpService httpService,
                         VendorLineService vendorLineService,
                         VendorPlayerService vendorPlayerService,
                         CurrencyService currencyService,
                         WalletService walletService) {

        this.gameService = gameService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.vendorPlayerService = vendorPlayerService;
        this.currencyService = currencyService;
        this.walletService = walletService;
    }

    @GetMapping(path = EndPoints.BALANCE)
    public ResponseVo<CommonVo> balance(HttpServletRequest request, @PathVariable("account") String account) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        String wToken = request.getHeader("wtoken");
        BalancePathVariableDto pathVariableDto = new BalancePathVariableDto();
        pathVariableDto.setAccount(account);

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        CommonVo commonVo = new CommonVo();
        Integer currencyId = 0;

        try {
            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(pathVariableDto, wToken);

            // 2. Get vendor player details
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(account);
            currencyId = vendorPlayer.getCurrencyId();
            GameSession gameSession = gameService.getGameSessionByUsername(account, null);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(vendorPlayer, wToken);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Construct VO
            commonVo.setBalance(balance);
            commonVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setData(commonVo);

        } catch (AuthenticationException authenticationException) {
            // due to vendor's abnormal behaviour of calling balance endpoint even after player has left the game
            // we will return balance as zero instead of throwing error to vendor
            try {
                Currency currency = currencyService.get(currencyId);
                commonVo.setCurrency(currency.getCode());

            } catch (InvalidCurrencyException invalidCurrencyException) {
                commonVo.setCurrency("PHP"); // just default to a random currency code
            }

            commonVo.setBalance(BigDecimal.ZERO);
            responseVo.setData(commonVo);

        } catch (CredentialNotFoundException | InvalidPlayerException |
                 InvalidVendorLineException playerNotFoundException) { // any other exception encountered

            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, playerNotFoundException);

        } catch (
                InvalidAgentApiCredentialException invalidAgentApiCredentialException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
                httpService.logError(httpRequestLog, invalidRequestException);
            }

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(BalancePathVariableDto pathVariableDto, String wToken) throws InvalidRequestException {
        // Validate value from Header and Path Variable

        if (wToken == null || pathVariableDto.getAccount() == null) {
            throw new InvalidRequestException();
        }

        // Validation with custom exception
        ValidationUtils.validateRequest(pathVariableDto);
    }

    private void doVerification(VendorPlayer vendorPlayer, String wToken) throws InvalidVendorLineException, CredentialNotFoundException {
        // 1. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(vendorPlayer.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 2. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
