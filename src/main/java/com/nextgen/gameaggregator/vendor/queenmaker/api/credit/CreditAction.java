package com.nextgen.gameaggregator.vendor.queenmaker.api.credit;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.dto.CreditTransactionsDto;
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
public class CreditAction {
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

    @PostMapping(path = EndPoints.WALLET_CREDIT)
    public CreditVo CreditAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        CreditVo creditVo = new CreditVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(creditDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<TransactionsVo>> futures = new LinkedList<>();
            for (CreditTransactionsDto transaction : creditDto.getTransactions()) {
                String traceId = httpRequestLog.getId();
                CompletableFuture<TransactionsVo> future = CompletableFuture.supplyAsync(() -> processData(transaction, clientId, clientSecret, traceId));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<TransactionsVo> transactionsList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            creditVo.setTransactions(transactionsList);


        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, creditVo);
        }

        return creditVo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditTransactionsDto transactionsDto, GameSession gameSession, String clientId, String clientSecret)
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

        // 5. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = gameSession.getVendorGameCode().split("_", 2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(transactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        ValidationUtils.isEquals(transactionsDto.getGpcode(), gpcode, InvalidVendorLineException::new);
        ValidationUtils.isEquals(transactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);

    }

    private TransactionsVo processData(CreditTransactionsDto transactionsDto, String clientId, String clientSecret, String traceId) {
        TransactionsVo transactionsVo = new TransactionsVo();

        try {
            // 1. Validate each user data
            this.doValidation(transactionsDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transactionsDto.getUserid(), transactionsDto.getGamecode());

            // 3. Verify Credential and Currency
            this.doVerification(transactionsDto, gameSession, clientId, clientSecret);

            // 4. Retrieve the latest wallet balance from Operator
//            BigDecimal balance = walletService.getBalance(traceId, gameSession);


            // 6. Set UsersVo
            transactionsVo.setTxid(traceId);


        } catch (Exception exception) {

            transactionsVo.setTxid(traceId);

        }

        return transactionsVo;
    }

}
