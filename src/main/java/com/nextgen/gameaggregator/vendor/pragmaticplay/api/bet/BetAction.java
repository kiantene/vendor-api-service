package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.event.EventDispatcher;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.operator.wallet.bet.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private BetService betService;
    @Autowired
    private WalletBetAction walletBetAction;
    @Autowired
    private EventDispatcher eventDispatcher;

    @PostMapping(path = Endpoints.BET)
    public ResponseVo betRequest(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        BetVo responseVo = new BetVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            BetDto dto = HttpService.convertQueryStringToDto(body, BetDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
//            GameSession session = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
//            String secretKey = vendorLineService.getCredentialValueByName(session.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
//            VendorService.validateHash(body, secretKey);

//            // Get seamless bet request Id
//            String seamlessBetRequestId = betService.getSeamlessBetRequestId(dto);

//            if (seamlessBetRequestId == null){
//                // If not finding seamless bet request id, then create one
//                seamlessBetRequestId = betService.createLogSeamlessBetHistoryRequest(dto, requestBody);
//            }

//            // Create kafka seamless bet history request data for data transforming
//            betService.createRecordToKafkaBetHistoryTopic(seamlessBetRequestId, authenticatedUser, requestBody);

            // Call bet request operator GRPC to get the balance of the player
//            BigDecimal balance = betService.getBetRequestBalanceFromGRPC(dto, traceId, authenticatedUser, seamlessBetRequestId);

            String currencyCode = "CNY";

            // Prepare data to be sent to Operator
            WalletBetDto walletBetDto = new WalletBetDto();
            walletBetDto.setTraceId(traceId);
            walletBetDto.setUsername(dto.getUserId()); // TODO: to update to the correct value
            walletBetDto.setTransactionId(traceId);
            walletBetDto.setExternalTransactionId(dto.getReference());
            walletBetDto.setAmount(dto.getAmount());
            walletBetDto.setCurrency(currencyCode); // TODO: to update to the correct value
            walletBetDto.setToken(""); // TODO: to update to the correct value
            walletBetDto.setGameId(dto.getGameId()); // TODO: to update to the correct value
            walletBetDto.setRoundId(dto.getRoundId());
            walletBetDto.setTimestamp(dto.getTimestamp());

            log.info(walletBetDto.toString());
            OperatorResponseVo<WalletBetData> operatorResponseVo = walletBetAction.call(walletBetDto);

            // Emit event for additional asynchronous processing
            eventDispatcher.emit(getClass(), body);

            responseVo.setTransactionId(traceId.replace("-", ""));
            responseVo.setCurrency(currencyCode);
            responseVo.setCash(operatorResponseVo.getData().getBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            responseVo.setUsedPromo(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());

//        } catch (AuthenticationException authenticationException) {
//            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

//        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
//            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);

//        } catch (InvalidSignatureException invalidHashException) {
//            responseVo.setError(ResponseCodes.INVALID_HASH);

//        } catch (CreateLogSeamlessBetHistoryException createLogSeamlessBetHistoryException) {
//            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_RETRY);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setErrorMessage(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(1);
            }
            ConcurrencyService.THREAD_POOL.submit(() -> httpService.logResponse(httpRequestLog, responseVo, traceId));
        }

        return responseVo;
    }
}
