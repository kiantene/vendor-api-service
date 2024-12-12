package com.nextgen.gameaggregator.vendor.cq9.api.refund;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.core.WalletRequestServiceImpl;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletServiceImpl;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cq9.constant.Formats;
import com.nextgen.gameaggregator.vendor.cq9.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.cq9.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.cq9.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class RefundAction {
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final UnsettledBetService unsettledBetService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    private final WalletTransactionService walletTransactionService;

    public RefundAction(GameSessionService gameSessionService,
                        HttpService httpService,
                        VendorLineService vendorLineService,
                        WalletService walletService,
                        VendorService vendorService,
                        UnsettledBetService unsettledBetService,
                        OperatorWalletServiceImpl operatorWalletService,
                        WalletRequestServiceImpl walletRequestService,
                        WalletTransactionServiceImpl walletTransactionService) {

        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.unsettledBetService = unsettledBetService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
        this.walletTransactionService = walletTransactionService;
    }

    @PostMapping(path = EndPoints.REFUND, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseVo<CommonVo> refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        String traceId = httpRequestLog.getId();
        String wToken = request.getHeader("wtoken");

        // Construct Vo
        ResponseVo<CommonVo> responseVo = new ResponseVo<>();
        StatusVo statusVo = new StatusVo();
        responseVo.setStatus(statusVo);

        CommonVo commonVo = new CommonVo();
        String vendorCurrencyCode = null;
        WalletTransaction walletTransaction = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            RefundDto refundDto = HttpService.convertQueryStringToDtoUrlDecode(body, RefundDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(refundDto, wToken);

            // 2. Gather require data
            Integer vendorId = vendorService.findVendorByCode(Credentials.VENDOR_CODE).getId();

            try {
                this.doRollback(traceId, vendorId, wToken, refundDto, commonVo, httpRequestLog);
            } catch (BetNotFoundException e) {
                String externalTransactionId = refundDto.getMtcode();
                walletTransaction = walletTransactionService.getByVendorIdAndExternalTransactionId(vendorId, externalTransactionId);

                if (walletTransaction != null) {
                    this.dataMapper(walletRequest, walletTransaction);
                    walletRequest = operatorWalletService.betCredit(walletRequest);
                    commonVo.setBalance(walletRequest.getBalanceAfter());
                    commonVo.setCurrency(walletRequest.getCurrencyCode());
                } else {
                    throw new BetNotFoundException();
                }
            }

            responseVo.setData(commonVo);

        } catch (BetNotFoundException betNotFoundException) {
            statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            //if found the bet in settled status
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                statusVo.setCode(ResponseCodes.SERVER_ERROR);

            } else {
                //if found the bet other in settled status (cancel / refund)
                commonVo.setBalance(betResultIdempotentViolationException.getBalance());
                commonVo.setCurrency(vendorCurrencyCode);
                responseVo.setData(commonVo);

            }
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                statusVo.setCode(ResponseCodes.TRANSACTION_RECORD_NOT_FOUND);
            } else {
                //Other operator errors
                statusVo.setCode(ResponseCodes.SERVER_ERROR);
            }

            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (AuthenticationException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 InvalidVendorLineException playerNotFoundException) {
            statusVo.setCode(ResponseCodes.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, playerNotFoundException);

        } catch (InvalidRequestException invalidRequestException) {
            statusVo.setCode(ResponseCodes.PARAMETER_ERROR);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }

            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (Exception exception) { // any other exception encountered
            statusVo.setCode(ResponseCodes.SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);
            walletRequest.setErrorMessage(exception.getMessage());

        } finally {
            statusVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(statusVo.getCode()));
            statusVo.setDateTime(new SimpleDateFormat(Formats.DATE_TIME_FORMAT).format(new Date()));

            if (walletTransaction != null) {
                walletRequestService.end(walletRequest, httpRequestLog, responseVo);
            } else {
                httpService.end(httpRequestLog, responseVo);
            }
        }

        return responseVo;
    }

    private void dataMapper(WalletRequest walletRequest, WalletTransaction walletTransaction) throws InvalidPlayerException, BetNotAllowedException, InternalServerException {
        String username = walletTransaction.getVendorPlayerUsername();
        String gameCode = walletTransaction.getVendorGameCode();
        Integer currencyId = walletTransaction.getCurrencyId();

        walletRequestService.updateByVendorUsername(walletRequest, username);
        walletRequestService.updateByVendorGameCode(walletRequest, gameCode, false);
        walletRequestService.updateByCurrencyId(walletRequest, currencyId);

        walletRequest.setVendorBetId(walletTransaction.getVendorBetId());
        walletRequest.setVendorGameCode(walletTransaction.getVendorGameCode());
        walletRequest.setVendorPlayerUsername(walletTransaction.getVendorPlayerUsername());
        walletRequest.setVendorId(walletTransaction.getVendorId());
        walletRequest.setCurrencyId(walletTransaction.getCurrencyId());
        walletRequest.setToken(walletTransaction.getToken());

        walletRequest.setExternalTransactionId(walletTransaction.getExternalTransactionId());
        walletRequest.setRoundId(walletTransaction.getRoundId());
        walletRequest.setTimestamp(walletTransaction.getTimestamp());
        walletRequest.setVendorBetId(walletTransaction.getVendorBetId());
        walletRequest.setTransferAmount(walletTransaction.getTransferAmount());
        walletRequest.setBetAmount(walletTransaction.getTransferAmount());
        walletRequest.setWinAmount(BigDecimal.ZERO);
        walletRequest.setEffectiveTurnover(walletTransaction.getTransferAmount());
        walletRequest.setJackpotAmount(BigDecimal.ZERO);
        walletRequest.setResultType(ResultType.BET_WIN.code);
        walletRequest.setBetStatus(BetStatus.REFUNDED);
        walletRequest.setVendorBetTime(walletTransaction.getTimestamp());
        walletRequest.setVendorSettleTime(walletTransaction.getTimestamp());
    }

    private void doRollback(String traceId, Integer vendorId, String wToken, RefundDto refundDto, CommonVo commonVo, HttpRequestLog httpRequestLog) throws
            BetNotFoundException, AuthenticationException, InvalidVendorLineException, CredentialNotFoundException, InvalidAgentApiCredentialException,
            RecordNotFoundException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, BetRefundIdempotentViolationException,
            TransactionStillProcessingException, InvalidOperatorResponseException, InvalidFormatException {

        UnsettledBet unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(vendorId, refundDto.getMtcode());
        String token = unsettledBet.getGameSessionToken();

        // 3. Verify session token
        GameSession gameSession = gameSessionService.verifyToken(token);
        String vendorCurrencyCode = gameSession.getVendorCurrencyCode();

        // 4. Verify remaining parameters (Verify against database values)
        this.doVerification(wToken, gameSession);

        // 5. Send refund to Operator
        BigDecimal balance = walletService.processRollback(traceId, refundDto, gameSession, vendorService, httpRequestLog);

        commonVo.setBalance(balance);
        commonVo.setCurrency(vendorCurrencyCode);
    }

    private void doValidation(RefundDto refundDto, String wToken) throws InvalidRequestException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(refundDto);
    }

    private void doVerification(String wToken, GameSession gameSession) throws InvalidVendorLineException, CredentialNotFoundException {
        // 3. Retrieve vendor line credentials and secretKey for verify API Token
        String walletToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.WALLET_TOKEN);

        // 4. Validate request Wallet Token
        ValidationUtils.isEquals(walletToken, wToken, InvalidVendorLineException::new);
    }
}
