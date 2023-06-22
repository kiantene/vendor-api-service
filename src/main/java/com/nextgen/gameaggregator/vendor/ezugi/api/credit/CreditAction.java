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
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
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
import java.time.format.DateTimeParseException;
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

            // Process result settled or cancelled bet data
            BigDecimal balance = BigDecimal.ZERO;
            switch (creditDto.getReturnReason()) {
                case ReturnReasons.CANCEL_BET, ReturnReasons.CANCELED_ROUND:
                    balance = walletService.processRollback(traceId, creditDto, gameSession, vendorService);
                    break;
                default:
                    ResultType resultType = getResultType(creditDto);
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
        } catch (BetRefundIdempotentViolationException e) {
            creditVo.setErrorCode(ResponseCodes.OK);
            creditVo.setErrorDescription("Transaction already processed");
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Debit Transaction already processed");
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            creditVo.setErrorCode(ResponseCodes.TRANSACTION_NOT_FOUND);
            creditVo.setErrorDescription("Debit transaction ID not found");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Invalid parameter");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Invalid Hash");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidFormatException e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Invalid Bet Type");
            httpService.logError(httpRequestLog, e);
        } catch (SettledBetIdempotentViolationException settledBetIdempotentViolationException) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Debit transaction already processed");
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            creditVo.setErrorDescription("Credit transaction is still processing");
        } catch (MergedBetDataIntegrityException | RecordNotFoundException | InvalidAgentApiCredentialException |
                 CredentialNotFoundException | InvalidKeyException | NoSuchAlgorithmException |
                 InvalidOperatorResponseException | CouchbaseDataIntegrityException e) {
            creditVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (creditVo.getErrorDescription() == null) {
                creditVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(creditVo.getErrorCode()));
            }
            httpService.end(httpRequestLog, creditVo);
        }
        return creditVo;
    }

    private void doValidation(CreditDto creditDto) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        // General validation
        ValidationUtils.validateRequest(creditDto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request)
            throws InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException, NoSuchAlgorithmException, InvalidKeyException, InvalidFormatException, InvalidRequestException {
        // Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getTableId(), AuthenticationException::new);

        // Verify Operator Id from vendor given
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_ID);
        ValidationUtils.isEquals(operatorId, String.valueOf(dto.getOperatorId()), InvalidRequestException::new);

        // Verify Signature key from vendor given
        String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        com.nextgen.gameaggregator.vendor.ezugi.service.VendorService.verifyHash(hashKey, httpRequestLog.getRequestBody(), request.getHeader("hash"));

        // Verify valid bet type id
        VendorService.verifyCreditBetTypeId(dto.getBetTypeID());
    }

    private ResultType getResultType(CreditDto dto) {
        ResultType resultType = ResultType.END;

        BigDecimal winAmount = Optional.ofNullable(dto.getWinAmount()).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;

        if (isWinAmountMoreThanZero) { // Win Amount > 0 ~ BET_WIN
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
