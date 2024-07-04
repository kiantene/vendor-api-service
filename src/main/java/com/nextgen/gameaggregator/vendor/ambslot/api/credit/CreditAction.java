package com.nextgen.gameaggregator.vendor.ambslot.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
import com.nextgen.gameaggregator.vendor.ambslot.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.DataVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.StatusVo;
import com.nextgen.gameaggregator.vendor.ambslot.vo.WalletVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public CreditAction(HttpService httpService,
                        VendorLineService vendorLineService,
                        GameSessionService gameSessionService,
                        WalletService walletService,
                        VendorService vendorService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;

        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.PAYOUT)
    public CreditVo payout(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        CreditVo creditVo = new CreditVo();
        StatusVo statusVo = new StatusVo();
        BalanceVo balanceVo = new BalanceVo();
        DataVo dataVo = new DataVo();
        WalletVo walletVo = new WalletVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // get x-ambslot-signature value for validation
            String signature = httpService.getHeadersInfo(request).get(EndPoints.HEADER_SIGNATURE);

            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(creditDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession, signature, body);

            ResultType resultType = VendorService.generateResultType(creditDto.getAmount());

            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            // set default value as after credit processes
            BigDecimal beforeBetBalance = balance;

            // if win then need to minus win amount to get before balance
            if (creditDto.getAmount() != BigDecimal.ZERO) {
                beforeBetBalance = beforeBetBalance.subtract(creditDto.getAmount());
            }

            statusVo.setCode(ResponseCodes.SUCCESS);
            statusVo.setMessage(ResponseCodes.SUCCESS_MSG);

            String dateTime = VendorService.convertUnixToDateTime(System.currentTimeMillis());

            walletVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            walletVo.setLastUpdate(dateTime);

            balanceVo.setBefore(beforeBetBalance.setScale(2, RoundingMode.DOWN));
            balanceVo.setAfter(balance.setScale(2, RoundingMode.DOWN));

            dataVo.setUsername(creditDto.getUsername());
            dataVo.setWallet(walletVo);
            dataVo.setBalance(balanceVo);
            dataVo.setRefId(creditDto.getTransactionId());

            creditVo.setData(dataVo);
        } catch (TransactionStillProcessingException |
                 BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.DUPLICATED_TRANSACTION_ERROR);
            statusVo.setMessage(ResponseCodes.DUPLICATED_TRANSACTION_ERROR_MSG);

        } catch (InvalidCredentialsException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_AGENT);
            statusVo.setMessage(ResponseCodes.INVALID_AGENT_MSG);

        } catch (InvalidRequestException |
                 JsonProcessingException |
                 GameNotSupportedException |
                 CurrencyNotSupportedException |
                 BetNotFoundException |
                 InvalidSignatureException |
                 CredentialNotFoundException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_REQUEST);
            statusVo.setMessage(ResponseCodes.INVALID_REQUEST_MSG);

        } catch (AuthenticationException |
                 InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.INVALID_USERNAME_OR_TOKEN);
            statusVo.setMessage(ResponseCodes.INVALID_USERNAME_OR_TOKEN_MSG);

        } catch (VendorCurrencyNotSupportException |
                 InsufficientBalanceException |
                 InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 MergedBetDataIntegrityException |
                 DisabledGameException e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

            statusVo.setCode(ResponseCodes.RESPONSE_ERROR);
            statusVo.setMessage(ResponseCodes.RESPONSE_ERROR_MSG);

        } finally {
            creditVo.setStatus(statusVo);
            httpService.end(httpRequestLog, creditVo);
        }

        return creditVo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession, String signature, String body) throws DisabledGameException, DisabledAgentPlayerException, DisabledVendorLineException, InvalidPlayerException, GameNotSupportedException, CurrencyNotSupportedException, CredentialNotFoundException, InvalidRequestException, InvalidSignatureException, JsonProcessingException, InvalidCredentialsException {
        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify vendor gameCode
        String gameCode = VendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(gameCode, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify agent is same with credential
        String agent = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.prefix);
        ValidationUtils.isEquals(agent.toLowerCase(), dto.getAgent(), InvalidCredentialsException::new);

        // Verify header value
        String secret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.secret);
        VendorService.validateSignature(signature, body, secret);
    }
}
