package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
public class BalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseEntity<BalanceVo> balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BalanceVo balanceVo = new BalanceVo();
        BalanceErrorVo balanceErrorVo = new BalanceErrorVo();
        BalanceDataVo balanceDataVo = new BalanceDataVo();
        String traceId = httpRequestLog.getId();
        String sign = request.getHeader(EndPoints.HEADER_SIGNATURE);
        String body = httpRequestLog.getRequestBody();
        Integer status = HttpStatus.SC_OK;

        try {

            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // Get and set req id
            String reqId = dto.getReqId();
            balanceVo.setReqId(reqId);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto, sign);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getUserToken());

            // Verify request parameters
            this.doVerification(dto, gameSession, sign, body);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            BalanceDataWalletVo balanceDataWalletVo = new BalanceDataWalletVo();

            // Set Balance and Currency
            balanceDataWalletVo.setBalance(balance);
            balanceDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());

            // Set BalanceDataWalletVo Object
            balanceDataVo.setWallet(balanceDataWalletVo);
            balanceVo.setStatus(status);

        } catch(AuthenticationException | InvalidVendorLineException |  CredentialNotFoundException tokenNotFoundOrInvalidException) {
            balanceErrorVo.setCode(ResponseCodes.USER_TOKEN_NOT_FOUND_OR_INVALID);
            balanceVo.setStatus(HttpStatus.SC_UNAUTHORIZED);
        } catch(GameNotSupportedException gameNotSupportedException) {
            balanceErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch(DisabledGameException disabledGameException) {
            balanceErrorVo.setCode(ResponseCodes.GAME_NOT_AVAILABLE);
            balanceVo.setStatus(HttpStatus.SC_FORBIDDEN);
        } catch(InvalidRequestException | CurrencyNotSupportedException | JsonProcessingException parameterInvalidException) {
            balanceErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            balanceVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch(InvalidAgentApiCredentialException | DisabledVendorLineException |
                 DisabledAgentPlayerException | InvalidPlayerException userNotFoundException) {
            balanceErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch(InvalidOperatorResponseException invalidOperatorResponseException) {
            balanceErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch(Exception exception) {
            balanceErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            balanceVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if(balanceVo.getStatus() == HttpStatus.SC_OK) {
                balanceVo.setData(balanceDataVo);
            } else {
                status = balanceVo.getStatus();
                balanceErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(balanceErrorVo.getCode()));
                balanceVo.setError(balanceErrorVo);
            }
            httpService.end(httpRequestLog, balanceVo);
        }

        return new ResponseEntity<>(balanceVo, HttpStatusCode.valueOf(status));

    }

    private void doValidation(BalanceDto dto, String token) throws InvalidRequestException {
        Optional.ofNullable(token).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession, String token, String body)
            throws AuthenticationException, InvalidPlayerException, GameNotSupportedException,
            CurrencyNotSupportedException, DisabledVendorLineException, DisabledAgentPlayerException,
            DisabledGameException, InvalidVendorLineException, CredentialNotFoundException, JsonProcessingException {

        // Convert object to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(body, Map.class);

        // Get signature key
        String signatureKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SIGNATURE_KEY);

        // Verify signature
        VendorService.validateSignature(token, bodyObj, signatureKey);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // Verify currency + game code
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);


    }
}
