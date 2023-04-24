package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorPlayer;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;

    @GetMapping(path = EndPoints.BALANCE)
    public ResponseVo<CommonVo> balance(HttpServletRequest request, @PathVariable("account") String account) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        String wToken = request.getHeader("wtoken");
        BalancePathVariableDto pathVariableDto = new BalancePathVariableDto();
        pathVariableDto.setAccount(account);

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        try {
            CommonVo commonVo = new CommonVo();

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(pathVariableDto, wToken);

            // 2. Get vendor player details
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(account);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(vendorPlayer, wToken);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // 5. Get vendor line supported currency
            VendorLine vendorLine = vendorLineService.getVendorLineById(vendorPlayer.getVendorLineId());

            // Construct VO
            commonVo.setBalance(balance);
            commonVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setData(commonVo);

        } catch (AuthenticationException authenticationException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (
                InvalidAgentApiCredentialException invalidAgentApiCredentialException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.GAME_ACTION_ERROR);

        } catch (InvalidPlayerException invalidPlayerException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

        } catch (InvalidVendorLineException invalidVendorLineException) { // any other exception encountered
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);

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

    private void doValidation(BalancePathVariableDto pathVariableDto, String wToken) throws InvalidPlayerException, InvalidRequestException {
        // Validate value from Header and Path Variable
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);
        Optional.ofNullable(pathVariableDto.getAccount()).orElseThrow(InvalidRequestException::new);

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
