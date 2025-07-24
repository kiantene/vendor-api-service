package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Actions;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Channel;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferService {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;
    private final VendorGameService vendorGameService;

    public TransferService(HttpService httpService,
                           VendorLineService vendorLineService,
                           GameSessionService gameSessionService,
                           WalletService walletService,
                           VendorService vendorService,
                           ValidationService validationService,
                           OperatorWalletService operatorWalletService,
                           WalletRequestService walletRequestService,
                           VendorGameService vendorGameService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
        this.vendorGameService = vendorGameService;
    }

    private void dataMapper(WalletRequest walletRequest, TransferDto dto, GameSession gameSession, boolean debit) {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(dto.getAcctId());
        walletRequest.setExternalTransactionId(dto.getExternalTransactionId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(dto.getGameCode());
        walletRequest.setTimestamp(dto.getVendorBetTime());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getVendorBetId());
        walletRequest.setTransferAmount(dto.getAmount());
        walletRequest.setBetAmount(debit ? null : dto.getBetAmount());
        walletRequest.setWinAmount(debit ? null : dto.getWinAmount());
        walletRequest.setEffectiveTurnover(debit ? null : dto.getEffectiveTurnover());
        walletRequest.setJackpotAmount(debit ? null : BigDecimal.ZERO);
        walletRequest.setResultType(debit ? null : ResultType.BET_WIN.code);
        walletRequest.setVendorBetTime(dto.getVendorBetTime());
        walletRequest.setVendorSettleTime(debit ? null : dto.getVendorSettleTime());
    }

    public TransferVo transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getId();
        TransferVo transferVo = new TransferVo();
        String merchantCode = "";
        String merchantTxId = "";
        boolean isCancel = false;
        boolean requireDebit = false;
        GameSession gameSession = null;

        try {
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            transferVo.setTransferId(dto.getTransferId());
            transferVo.setMerchantCode(dto.getMerchantCode());
            transferVo.setSerialNo(traceId);
            transferVo.setAcctId(dto.getAcctId());
            this.doValidation(dto);

            // authenticate game session,verify and regenerate latest game code token
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
                if (dto.getGameCode() != null) {
                    gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameCode(), gameSession);
                }
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(dto.getAcctId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGameCode());
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            merchantCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            merchantTxId = gameSession.getToken();
            VendorGame vendorGame = vendorGameService.getByVendorGameId(gameSession.getVendorGameId());
            requireDebit = vendorGame.getRequireDebit();
            this.doVerification(dto, gameSession, merchantCode);

            switch (dto.getType()) {
                case Actions.PLACE_BET -> {
                    // Place bet action
                    if (!requireDebit) {
                        BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
                        transferVo.setBalance(betEvent.getLastBalance());

                    } else {
                        this.dataMapper(walletRequest, dto, gameSession, true);
                        walletRequest = operatorWalletService.betDebit(walletRequest);
                        transferVo.setBalance(walletRequest.getBalanceAfter());
                    }

                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.CANCEL_BET -> {
                    // Cancel bet and refund action
                    isCancel = true;
                    BigDecimal rollbackBalance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);
                    transferVo.setBalance(rollbackBalance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.PAYOUT -> {
                    // Payout action
                    if (!requireDebit) {
                        WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                        String type = Optional.ofNullable(dto.getSpecialGame()).map(SpecialGameDto::getType).orElse(null);
                        ResultType resultType = determineResultType(type, winDataDto);
                        BigDecimal payoutBalance = walletService.processBetResult(traceId, gameSession, winDataDto, resultType, vendorService, httpRequestLog);
                        transferVo.setBalance(payoutBalance);

                    } else {
                        this.dataMapper(walletRequest, dto, gameSession, false);
                        walletRequest = operatorWalletService.betCredit(walletRequest);
                        transferVo.setBalance(walletRequest.getBalanceAfter());
                    }

                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                default -> transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            }

            transferVo.setMerchantTxId(merchantTxId);

        } catch (AuthenticationException authenticationException) {
            // account not found 
            httpService.logError(httpRequestLog, authenticationException);
            transferVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);

        } catch (CredentialNotFoundException | UnableToFindCredentialsException |
                 InvalidPlayerException merchantNotFoundException) {
            // merchant not found
            httpService.logError(httpRequestLog, merchantNotFoundException);
            transferVo.setResponseCode(isCancel ? ResponseCode.INVALID_REQUEST : ResponseCode.SYSTEM_ERROR);

        } catch (DisabledVendorLineException | DisabledAgentPlayerException |
                 DisabledGameException | MergedBetDataIntegrityException serviceInaccessibleException) {
            // service inaccessible 
            httpService.logError(httpRequestLog, serviceInaccessibleException);
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (CurrencyNotSupportedException currencyInvalidException) {
            // invalid currency
            httpService.logError(httpRequestLog, currencyInvalidException);
            transferVo.setResponseCode(ResponseCode.CURRENCY_INVALID);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            // insufficient balance
            httpService.logError(httpRequestLog, insufficientBalanceException);
            transferVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (BetNotFoundException | RecordNotFoundException notFoundException) {
            // record ID not found
            httpService.logError(httpRequestLog, notFoundException);
            transferVo.setResponseCode(isCancel ? ResponseCode.RELATED_ID_NOT_FOUND : ResponseCode.RECORD_ID_NOT_FOUND);

        } catch (InvalidRequestException | InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException | GameNotSupportedException |
                 BetRefundIdempotentViolationException invalidRequestException) {
            // invalid request
            httpService.logError(httpRequestLog, invalidRequestException);
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (JsonProcessingException invalidFormatException) {
            // invalid format
            httpService.logError(httpRequestLog, invalidFormatException);
            transferVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (BetResultIdempotentViolationException e) {
            if (isCancel && e.getStatus().equals(BetStatus.SETTLED.code)) {
                //if the bet is settled but receiving refund request, should return error
                httpService.logError(httpRequestLog, e);
                transferVo.setResponseCode(ResponseCode.RELATED_ID_NOT_FOUND);
            } else {
                transferVo.setBalance(e.getBalance());
                transferVo.setMsg(ResponseCode.SUCCESS.description);
                transferVo.setResponseCode(ResponseCode.SUCCESS);
                transferVo.setMerchantTxId(merchantTxId);
            }

        } catch (Exception exception) {
            transferVo.setResponseCode(isCancel ? ResponseCode.INVALID_REQUEST : ResponseCode.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            if (!requireDebit) {
                httpService.end(httpRequestLog, transferVo);
            } else {
                walletRequestService.end(walletRequest, httpRequestLog, transferVo);
            }
        }

        return transferVo;
    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(TransferDto dto, GameSession gameSession, String merchantCode)
            throws
            UnableToFindCredentialsException,
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidRequestException, InvalidPlayerException {

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameCode()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //validate vendor username, agent vendor line, player status, and game status
        if (dto.getType().equals(Actions.PLACE_BET)) {
            validationService.validateEligibleBet(gameSession, dto.getAcctId());
        }

        // Verify channel
        if (!Channel.list.contains(dto.getChannel())) throw new InvalidRequestException();
    }

    private ResultType determineResultType(String type, WinDataDto winDataDto) {
        BigDecimal amount = winDataDto.getAmount();
        //free spin,bonus,free bonus,bonus free
        boolean isSpecialType = type != null;

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (isSpecialType && winDataDto.getSpecialGame().getSequence() == 0) {
                return ResultType.WIN;

            } else {
                return isSpecialType ? ResultType.BET_WIN : ResultType.WIN;

            }

        } else {
            return isSpecialType ? ResultType.BET_LOSE : ResultType.END;
        }
    }
}
