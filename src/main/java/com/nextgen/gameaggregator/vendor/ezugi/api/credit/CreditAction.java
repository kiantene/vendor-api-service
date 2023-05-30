package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ReturnReasons;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction extends CommonDto {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.CREDIT)
    public CommonVo credit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        CreditVo creditVo = new CreditVo();
        try {
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // Get and set bet game data Object from body
            this.setGameData(creditDto);

            // Validate request parameters (Non-database calls)
            this.doValidation(creditDto);

            // Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession, httpRequestLog, request);

            // Get unsettled bet
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(creditDto.getUid());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(creditDto.getTableId(), vendorPlayer.getVendorId());
            UnsettledBet unsettledBet = betHistoryService.getRawUnsettledBetByBetIdAndRoundIdAndGameIdAndPlayerId(creditDto.getVendorBetId(), creditDto.getRoundId(), vendorGame.getId(), vendorPlayer.getId());

            // Process result settled or cancelled bet data
            BigDecimal balance = BigDecimal.ZERO;
            switch (creditDto.getReturnReason()) {
                case ReturnReasons.CANCEL_BET, ReturnReasons.CANCELED_ROUND:
                    balance = walletService.processRollback(traceId, creditDto, gameSession, vendorService);
                    break;
                default:
                    ResultType resultType = getResultType(creditDto, unsettledBet);
                    balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);
                    break;
            }

            // Construct Vo
            creditVo.setToken(creditDto.getToken());
            creditVo.setOperatorId(creditDto.getOperatorId());
            creditVo.setUid(gameSession.getVendorPlayerUsername());
            creditVo.setRoundId(creditDto.getVendorRoundId());
            creditVo.setTransactionId(creditDto.getTransactionId());
            creditVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            creditVo.setCurrency(gameSession.getVendorCurrencyCode());
            creditVo.setErrorCode(ResponseCodes.OK);
            creditVo.setTimestamp(System.currentTimeMillis());
        } catch (AuthenticationException e) {
            creditVo.setErrorCode(ResponseCodes.TOKEN_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            creditVo.setErrorCode(ResponseCodes.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            creditVo.setErrorCode(ResponseCodes.USER_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            creditVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(creditVo.getErrorCode()));
            httpService.end(httpRequestLog, creditVo);
        }
        return creditVo;
    }

    private void doValidation(CreditDto creditDto) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        // General validation
        ValidationUtils.validateRequest(creditDto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request)
            throws InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException, NoSuchAlgorithmException, InvalidKeyException {
        // Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getTableId(), AuthenticationException::new);

        // Verify Signature key from vendor given
        String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        com.nextgen.gameaggregator.vendor.ezugi.service.VendorService.verifyHash(hashKey, httpRequestLog.getRequestBody(), request.getHeader("hash"));
    }

    private ResultType getResultType(CreditDto dto, UnsettledBet unsettledBet) {
        ResultType resultType = ResultType.LOSE;

        BigDecimal betAmount = Optional.ofNullable(unsettledBet.getBetAmount()).orElse(BigDecimal.ZERO);
        BigDecimal winAmount = Optional.ofNullable(dto.getWinAmount()).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isBetAmountEqualThanZero = betAmount.compareTo(BigDecimal.ZERO) == 0;
        boolean isWinAmountEqualThanZero = winAmount.compareTo(BigDecimal.ZERO) == 0;

        if (isWinAmountMoreThanZero) { // Win Amount > 0 ~ BET_WIN
            resultType = ResultType.WIN;
        }
        if (isBetAmountEqualThanZero && isWinAmountEqualThanZero) { // Win Amount == 0 and Bet Amount == 0 ~ BET_WIN
            resultType = ResultType.WIN;
        }
        return resultType;
    }

    private void setGameData(CreditDto creditDto) throws JsonProcessingException {
        GameDataStringDto gameDataStringDto = new GameDataStringDto();
        gameDataStringDto.setBetAmount(0.0);
        gameDataStringDto.setWinAmount(0.0);

        if (StringUtils.isNotBlank(creditDto.getGameDataString())) {
            gameDataStringDto = HttpService.convertJsonToDto(creditDto.getGameDataString(), GameDataStringDto.class);
        }
        creditDto.setGameDataStringDto(gameDataStringDto);
    }
}
