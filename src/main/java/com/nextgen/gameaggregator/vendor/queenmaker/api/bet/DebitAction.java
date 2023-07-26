package com.nextgen.gameaggregator.vendor.queenmaker.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = EndPoints.WALLET_DEBIT)
    public DebitVo DebitAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        DebitVo debitVo = new DebitVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(debitDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<TransactionsVo>> futures = new LinkedList<>();
            for (DebitTransactionsDto transaction : debitDto.getTransactions()) {
                String traceId = httpRequestLog.getId();
                CompletableFuture<TransactionsVo> future = CompletableFuture.supplyAsync(() -> processData(transaction, clientId, clientSecret, traceId, body));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<TransactionsVo> transactionsList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            debitVo.setTransactions(transactionsList);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, debitVo);
        }

        return debitVo;

    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(DebitTransactionsDto debitTransactionsDto, GameSession gameSession, String clientId, String clientSecret)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            AuthenticationException {

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitTransactionsDto.getUserid());

        // 2. Validate Credentials
        Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
        Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 3. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = gameSession.getVendorGameCode().split("_", 2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(debitTransactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        ValidationUtils.isEquals(debitTransactionsDto.getGpcode(), gpcode, InvalidVendorLineException::new);
        ValidationUtils.isEquals(debitTransactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);

    }

    private TransactionsVo processData(DebitTransactionsDto debitTransactionsDto, String clientId, String clientSecret, String traceId, String body) {
        TransactionsVo transactionsVo = new TransactionsVo();

        try {
            // 1. Validate each user data
            this.doValidation(debitTransactionsDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(debitTransactionsDto.getUserid(), debitTransactionsDto.getGamecode());

            // 3. Verify Credential and Currency
            this.doVerification(debitTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitTransactionsDto, body);

            // 5. Set UsersVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(traceId);
            transactionsVo.setBal(betEvent.getLastBalance());
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());

        } catch (Exception exception) {

            transactionsVo.setTxid(traceId);

        }

        return transactionsVo;
    }
}

