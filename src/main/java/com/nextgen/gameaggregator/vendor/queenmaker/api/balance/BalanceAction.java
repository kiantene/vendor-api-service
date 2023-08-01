package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.queenmaker.dto.UsersDto;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.UsersVo;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.WalletsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.WALLET_BALANCE)
    public BalanceVo BalanceAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        BalanceVo balanceVo = new BalanceVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            String body = httpRequestLog.getRequestBody();
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(balanceDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<UsersVo>> futures = new LinkedList<>();
            for (UsersDto user : balanceDto.getUsers()) {
                String traceId = httpRequestLog.getId();
                CompletableFuture<UsersVo> future = CompletableFuture.supplyAsync(() -> processData(user, clientId, clientSecret, traceId, httpRequestLog));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<UsersVo> usersList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            balanceVo.setUsers(usersList);

        } catch (InvalidRequestException invalidRequestException) {
            String message = Optional.ofNullable(invalidRequestException.getValidation().values().iterator().next()).orElse("");
            String errdesc = ResponseCodes.SYSTEM_ERROR.errdesc.replace(Formats.REPLACE_STRING, " : " + message);
            balanceVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, errdesc);
        } catch (JsonProcessingException jsonProcessingException) {
            balanceVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);
        } catch (Exception exception) {

            balanceVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(UsersDto usersDto, GameSession gameSession, String clientId, String clientSecret)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            InvalidCurrencyException, AuthenticationException {

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Validate Credentials
        Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
        Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 5. Validate Vendor Currency Code
        ValidationUtils.isEquals(usersDto.getCur(), gameSession.getVendorCurrencyCode(), InvalidCurrencyException::new);
    }

    private UsersVo processData(UsersDto usersDto, String clientId, String clientSecret, String traceId, HttpRequestLog httpRequestLog) {
        UsersVo usersVo = new UsersVo();

        try {
            // 1. Validate each user data
            this.doValidation(usersDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(usersDto.getUserid());

            // 3. Verify Credential and Currency
            this.doVerification(usersDto, gameSession, clientId, clientSecret);

            // 4. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);

            // 5. Set WalletVo for each user
            WalletsVo walletVo = new WalletsVo();
            walletVo.setCode(Formats.MAIN_WALLET_CODE);
            walletVo.setBal(balance);
            walletVo.setCur(gameSession.getVendorCurrencyCode());
            List<WalletsVo> walletsList = new LinkedList<>();
            walletsList.add(walletVo);

            // 6. Set UsersVo
            usersVo.setUserid(gameSession.getVendorPlayerUsername());
            usersVo.setWallets(walletsList);

        } catch (InvalidRequestException invalidRequestException) {
            String message = Optional.ofNullable(invalidRequestException.getValidation().values().iterator().next()).orElse("");
            String errdesc = ResponseCodes.INVALID_ARGUMENTS.errdesc.replace(Formats.REPLACE_STRING, message);
            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT, errdesc);
        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 CredentialNotFoundException |
                 InvalidVendorLineException |
                 InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException
                exception) {

            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);
        } catch (InvalidPlayerException | AuthenticationException e) {
            String errdesc = ResponseCodes.INVALID_ARGUMENTS.errdesc.replace(Formats.REPLACE_STRING, "invalid player");
            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, errdesc);
        } catch (InvalidCurrencyException invalidCurrencyException) {

            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCodes.CURRENCY_MISMATCH);
        } catch (Exception e) {
            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, usersVo);
        }

        return usersVo;
    }

}
