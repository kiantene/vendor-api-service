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
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.WALLET_BALANCE)
    public BalanceVo BalanceAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BalanceVo balanceVo = new BalanceVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);

            String traceId = httpRequestLog.getId();
            String body = httpRequestLog.getRequestBody();
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(balanceDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<UsersVo>> futures = new LinkedList<>();
            for (UsersDto user : balanceDto.getUsers()) {

                CompletableFuture<UsersVo> future = CompletableFuture.supplyAsync(() -> processData(user, clientId, clientSecret, traceId, httpRequestLog));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<UsersVo> usersList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            balanceVo.setUsers(usersList);

        } catch (InvalidRequestException e) {
            balanceVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, e.getValidation().values().iterator().next().toString());

        } catch (JsonProcessingException e) {
            balanceVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);

        } catch (Exception e) {
            balanceVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, balanceVo);
            log.info("QM Balance Request Log : " + httpRequestLog.getRequestBody());
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
            InvalidCurrencyException {

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
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
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 5. Set WalletVo for each user
            WalletsVo walletVo = new WalletsVo();
            walletVo.setCode(Formats.MAIN_WALLET_CODE);
            walletVo.setBal(balance);
            walletVo.setCur(gameSession.getVendorCurrencyCode());
            List<WalletsVo> walletsList = new LinkedList<>();
            walletsList.add(walletVo);

            // 6. Set UsersVo
            usersVo.setWallets(walletsList);

        } catch (AuthenticationException e) {
            usersVo.setResponseCode(ResponseCodes.INVALID_OR_EXPIRED_TOKEN);

        } catch (InvalidCurrencyException e) {
            usersVo.setResponseCode(ResponseCodes.CURRENCY_MISMATCH);

        } catch (InvalidRequestException e) {
            usersVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);

        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidVendorLineException |
                 InvalidAgentApiCredentialException |
                 CredentialNotFoundException e) {
            usersVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);

        } catch (InvalidPlayerException e) {
            usersVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Player");

        } catch (InvalidOperatorResponseException e) {
            usersVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");

        } catch (Exception e) {
            usersVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            usersVo.setUserid(usersDto.getUserid());
            httpService.end(httpRequestLog, usersVo);
            log.info("QM Balance Request Log : " + httpRequestLog);
        }

        return usersVo;
    }

}
