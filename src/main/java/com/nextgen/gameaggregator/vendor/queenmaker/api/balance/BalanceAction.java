package com.nextgen.gameaggregator.vendor.queenmaker.api.balance;

import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCode;
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


            List<CompletableFuture<UsersVo>> futures = new LinkedList<>();
            // 1. Validate request parameters (Non-database calls)
            this.doValidation(balanceDto);

            for (UsersDto user : balanceDto.getUsers()) {
                String traceId = httpRequestLog.getTraceId();
                CompletableFuture<UsersVo> future = CompletableFuture.supplyAsync(() -> processData(user, clientId, clientSecret, traceId));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<UsersVo> usersList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            balanceVo.setUsers(usersList);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                String errorMessage = invalidRequestException.getValidation().values().iterator().next();

//                balanceVo.setResponseCode(ResponseCode.INCORRECT_FORMAT, ResponseCode.RESPONSE_DESCRIPTION.get(ResponseCode.INCORRECT_FORMAT).replace(ResponseCode.INCORRECT_FORMAT_REPLACE_STRING, errorMessage));


                System.out.println(errorMessage);
            }
        } catch (Exception exception) { // any other exception encountered

            httpService.logError(httpRequestLog, exception);
            balanceVo.setResponseCode(ResponseCode.INCORRECT_FORMAT);


        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;
    }

    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(balanceDto);
    }

    private void doVerification(UsersDto user, RawGameSession rawGameSession, String clientId, String clientSecret)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidVendorLineException {

        // Validate value from Header and Path Variable
        Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
        Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);

        String CLIENT_ID = vendorLineService.getCredentialValueByName(rawGameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(rawGameSession.getVendorLineId(), Credentials.CLIENT_SECRET);

        // 3. Validate request Wallet Token
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(rawGameSession, user.getUserid());

    }

    private UsersVo processData(UsersDto usersDto, String clientId, String clientSecret, String traceId) {
        UsersVo usersVo = new UsersVo();

        try {
            // 2. Verify session token
            RawGameSession rawGameSession = gameSessionService.getGameSessionByVendorPlayerUsername(usersDto.getUserid());

            this.doVerification(usersDto, rawGameSession, clientId, clientSecret);

            // 3. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, rawGameSession);

            // Set WalletVo for each user
            WalletsVo walletVo = new WalletsVo();
            walletVo.setCode(Formats.MAIN_WALLET_CODE);
            walletVo.setBal(balance);
            walletVo.setCur(rawGameSession.getVendorCurrencyCode());
            List<WalletsVo> walletsList = new LinkedList<>();
            walletsList.add(walletVo);

            // Set UsersVo
            usersVo.setUserid(rawGameSession.getVendorPlayerUsername());
            usersVo.setWallets(walletsList);
        } catch (Exception e) {
            usersVo.setUserid(usersDto.getUserid());
            usersVo.setResponseCode(ResponseCode.INCORRECT_FORMAT);

        }

        return usersVo;
    }

}
