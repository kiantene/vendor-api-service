package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TransferAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private BetResultLogService betResultLogService;

    @PostMapping(path = EndPoints.TRANSFER)
    public TransferVo transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        TransferVo responseVo = new TransferVo();
        FundTransferResponseVo fundTransferResponseVo = new FundTransferResponseVo();
        StatusVo statusVo = new StatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            TransferDto transferDto = HttpService.convertJsonToDto(body, TransferDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(transferDto);

            GameSession gameSession = new GameSession();
            if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto() != null) {
                //check 1st fundinfo gamestatemode value
                if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0].getGameStateMode() != 3) {
                    //Get GameSession by token
                    gameSession = gameSessionService.verifyToken(transferDto.getFundTransferRequestDto().getToken());
                } else {
                    //When gamestatemode = 3 get GameSession by player name and vendor game id, this end request might send out after few days of bet
                    gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transferDto.getFundTransferRequestDto().getAccountId(), transferDto.getBaseGame().getKeyName());
                }
            } else {
                //Get GameSession by token
                gameSession = gameSessionService.verifyToken(transferDto.getFundTransferRequestDto().getToken());
            }


            //Verify remaining parameters (Verify against database values)
            this.doVerification(transferDto, gameSession);

            //setup debit and credit bet type respond message
            if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
                statusVo.setSuccessDebit(false);
                statusVo.setSuccessCredit(false);
            }
            BigDecimal balance = new BigDecimal(0);

            //check not refund/recredit transfer
            if (!transferDto.getFundTransferRequestDto().getIsRetry()) {
                //Loop bet info
                for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                    if (fundInfoDto.getGameStateMode() == 1) {
                        //process bet result into unsettle bet when gamestatemode = 1(game round start)
                        Boolean betResult = processBet(fundInfoDto, transferDto.getFundTransferRequestDto(), transferDto.getBaseGame().getKeyName(), gameSession, traceId, body);
                        //setup debit and credit bet type respond message
                        if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
                            statusVo.setSuccessDebit(betResult);
                        }
                    } else {
                        //process bet result into settle bet when gamestatemode = 2(game round end/ bonus free spin) or 0(free spin/jackpot) or 3(expire bet round end)
                        Boolean settleResult = processBonusAndSettle(fundInfoDto, transferDto.getFundTransferRequestDto(), transferDto.getBaseGame().getKeyName(), fundInfoDto.getGameStateMode(), gameSession, traceId, httpRequestLog);
                        //setup debit and credit bet type respond message
                        if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
                            statusVo.setSuccessCredit(settleResult);
                        }
                    }
                }
            } else {
                //handle refund and recredit
                FundInfoDto fundInfoDto = new FundInfoDto();
                Boolean settleBetAvailable = false;
                Boolean unsettleBetAvailable = false;

                //check Available Unsettle Bet for recredit and refund
                if (transferDto.getFundTransferRequestDto().getIsRecredit()) {
                    //handle recredit condition
                    //get 1st array object as record
                    fundInfoDto = transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0];

                    //check settle bet available
                    settleBetAvailable = checkBetAvailable(gameSession.getVendorPlayerId(), null, fundInfoDto.getInitialDebitTransferId(), "", "SETTLE_BET");
                    if (!settleBetAvailable) {
                        //check unsettle bet result available
                        unsettleBetAvailable = checkBetAvailable(gameSession.getVendorPlayerId(), gameSession.getVendorGameId(), fundInfoDto.getOriginalTransferId(), transferDto.getFundTransferRequestDto().getGameInstanceId(), "UNSETTLE_BET_RESULT");
                        if (!unsettleBetAvailable) {
                            //when no unsettle bet result found, recredit to player
                            Boolean settleResult = processBonusAndSettle(fundInfoDto, transferDto.getFundTransferRequestDto(), transferDto.getBaseGame().getKeyName(), fundInfoDto.getGameStateMode(), gameSession, traceId, httpRequestLog);
                        }
                    } else {
                        //return error when settle bet available
                        throw new BetNotFoundException();
                    }

                } else {
                    //handle refund condition
                    //check settle bet available
                    settleBetAvailable = checkBetAvailable(gameSession.getVendorPlayerId(), null, transferDto.getFundTransferRequestDto().getFundDto().getRefundDto().getOriginalTransferId(), "", "SETTLE_BET");

                    if (!settleBetAvailable) {
                        //check unsettle bet available
                        unsettleBetAvailable = checkBetAvailable(gameSession.getVendorPlayerId(), null, transferDto.getFundTransferRequestDto().getFundDto().getRefundDto().getOriginalTransferId(), "", "UNSETTLE_BET");

                        if (unsettleBetAvailable) {
                            //handle when unsettle bet available, refund and void the game
                            balance = walletService.processRollback(traceId, transferDto.getFundTransferRequestDto().getFundDto().getRefundDto(), gameSession, vendorService);
                            //void the game
                            statusVo.setRefundStatus(1);
                        } else {
                            //handle when unsettle bet not available, no action and void the game
                            statusVo.setRefundStatus(2);
                        }
                    } else {
                        //return error when settle bet available
                        throw new BetNotFoundException();
                    }
                }

            }

            //Get walletBalance
            balance = walletService.getBalance(traceId, gameSession);

            //return success respond
            statusVo.setSuccess(true);
            fundTransferResponseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            fundTransferResponseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());

        } catch (
                InvalidAgentApiCredentialException |
                RecordNotFoundException |
                AuthenticationException |
                BetResultIdempotentViolationException |
                MergedBetDataIntegrityException |
                BetRefundIdempotentViolationException |
                BetNotFoundException |
                CouchbaseDataIntegrityException |
                JsonProcessingException |
                InvalidPlayerException |
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidRequestException |
                NoAvailableLineException |
                CredentialNotFoundException |
                DisabledVendorLineException generalException
        ) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            statusVo.setSuccess(false);
            statusVo.setNoFunds(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
            httpService.logError(httpRequestLog, exception);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;

    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBaseGame());
        ValidationUtils.validateRequest(dto.getSubAuth());
        ValidationUtils.validateRequest(dto.getFundTransferRequestDto());
        ValidationUtils.validateRequest(dto.getFundTransferRequestDto().getFundDto());
        if (dto.getFundTransferRequestDto().getFundDto().getFundInfoDto() != null) {
            //Loop bet info
            for (FundInfoDto fundInfoDto : dto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                ValidationUtils.validateRequest(fundInfoDto);
                //date time format validation
                if (!vendorService.isValidDateString(fundInfoDto.getDtEvent())) {
                    throw new InvalidRequestException();
                }
            }
        }
        if (dto.getFundTransferRequestDto().getFundDto().getRefundDto() != null) {
            ValidationUtils.validateRequest(dto.getFundTransferRequestDto().getFundDto().getRefundDto());
            //date time format validation
            if (!vendorService.isValidDateString(dto.getFundTransferRequestDto().getFundDto().getRefundDto().getDtEvent())) {
                throw new InvalidRequestException();
            }
        }
        ValidationUtils.isEquals("fundtransferrequest", dto.getType(), InvalidRequestException::new);
        //date time format validation
        if (!vendorService.isValidDateString(dto.getDtSent())) {
            throw new InvalidRequestException();
        }


    }

    private void doVerification(TransferDto dto, GameSession gameSession)
            throws NoAvailableLineException, CredentialNotFoundException, InvalidPlayerException,
            AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException {

        //Verify received passkey is the same from credential
        String passkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSKEY);
        ValidationUtils.isEquals(passkey, dto.getSubAuth().getPasskey(), NoAvailableLineException::new);

        //Verify received brand id is the same from credential
        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        ValidationUtils.isEquals(brandId, dto.getSubAuth().getBrandid(), NoAvailableLineException::new);

        //Verify vendor game code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getBaseGame().getKeyName(), NoAvailableLineException::new);

        //Verify vendor currency code is the same from gameSession
        if (dto.getFundTransferRequestDto().getFundDto().getFundInfoDto() != null) {
            //Loop bet info
            for (FundInfoDto fundInfoDto : dto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), fundInfoDto.getCurrencyCode(), NoAvailableLineException::new);
            }
        }
        if (dto.getFundTransferRequestDto().getFundDto().getRefundDto() != null) {
            ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getFundTransferRequestDto().getFundDto().getRefundDto().getCurrencyCode(), NoAvailableLineException::new);
        }

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getFundTransferRequestDto().getAccountId());

    }

    private boolean processBet(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, String gameId, GameSession gameSession, String traceId, String body)
            throws InsufficientBalanceException, CouchbaseDataIntegrityException, InvalidOperatorResponseException,
            InvalidAgentApiCredentialException, BetResultIdempotentViolationException, TransactionStillProcessingException {

        // Construct bet Dto
        BetDto betDto = new BetDto();
        betDto.setExternalTransactionId(dto.getTransferId());
        betDto.setVendorBetId(fundTransferRequestDto.getFriendlyGameInstanceId());
        betDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
        betDto.setGameId(gameId);
        betDto.setBetAmount(dto.getAmount().abs());
        betDto.setWinAmount(null);
        betDto.setWinLoss(null);
        betDto.setEffectiveTurnover(null);
        betDto.setRawVendorBetTime(dto.getDtEvent());
        betDto.setRawResultTime(dto.getDtEvent());
        betDto.setRawVendorSettleTime(dto.getDtEvent());
        betDto.setJackpotAmount(null);
        betDto.setIsFreespin(0);
        betDto.setBetStatus(BetStatus.UNSETTLED);

        //process unsettle bet data
        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

        return true;
    }

    private boolean processBonusAndSettle(FundInfoDto dto, FundTransferRequestDto fundTransferRequestDto, String gameId, Integer type, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog)
            throws BetNotFoundException, InvalidOperatorResponseException, TransactionStillProcessingException,
            InvalidAgentApiCredentialException, MergedBetDataIntegrityException, InsufficientBalanceException,
            BetResultIdempotentViolationException {

        // Construct bet Dto
        BetDto betDto = new BetDto();
        betDto.setExternalTransactionId(dto.getTransferId());
        betDto.setVendorBetId(fundTransferRequestDto.getFriendlyGameInstanceId());
        betDto.setRoundId(fundTransferRequestDto.getGameInstanceId());
        betDto.setGameId(gameId);
        betDto.setBetAmount(null);
        betDto.setWinLoss(null);
        betDto.setEffectiveTurnover(null);
        betDto.setRawVendorBetTime(null);
        betDto.setRawResultTime(dto.getDtEvent());
        betDto.setRawVendorSettleTime(dto.getDtEvent());


        ResultType resultType = ResultType.WIN;
        if (type == 2 || type == 3) {
            //handle settle bet and bonus free spin
            betDto.setWinAmount(dto.getAmount().abs());
            betDto.setJackpotAmount(null);
            betDto.setBetStatus(BetStatus.SETTLED);
            if (dto.getIsBonus() == true) {
                //bonus free spin will be settled without bet
                //betDto.setVendorBetId(dto.getTransferId());
                betDto.setBetAmount(BigDecimal.valueOf(0));
                betDto.setRawVendorBetTime(dto.getDtEvent());
                betDto.setIsFreespin(1);
                resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.BET_WIN : ResultType.BET_LOSE;
            } else {
                //settle bet will settle with initial bet betId
                //betDto.setVendorBetId(dto.getInitialDebitTransferId());
                betDto.setIsFreespin(0);
                resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.END;
            }
        } else {
            //type = 0, handle jackpot and free spin
            betDto.setBetStatus(BetStatus.UNSETTLED);
            resultType = dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;
            //betDto.setVendorBetId(dto.getTransferId());
            if (dto.getJpWin()) {
                //if JPwin = TRUE, handle jackpot
                betDto.setWinAmount(BigDecimal.valueOf(0));
                betDto.setJackpotAmount(dto.getAmount().abs());
                betDto.setIsFreespin(0);
            } else {
                //handle free spin
                betDto.setWinAmount(dto.getAmount().abs());
                betDto.setJackpotAmount(null);
                betDto.setIsFreespin(1);
            }
        }

        //process bet result data (settle or unsettle)
        BigDecimal balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

        return true;
    }

    private Boolean checkBetAvailable(Long vendorPlayerId, Integer vendorGameId, String transactionId, String roundId, String type) {

        if (type == "SETTLE_BET") {
            try {
                //check settle bet available
                SettledBet settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, transactionId);
                return true;
            } catch (BetNotFoundException betNotFoundException) {
                return false;
            }
        }

        if (type == "UNSETTLE_BET") {
            try {
                //check unsettle bet available
                UnsettledBet unsettledBet = unsettledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, transactionId);
                return true;
            } catch (BetNotFoundException betNotFoundException) {
                return false;
            }
        }

        if (type == "UNSETTLE_BET_RESULT") {
            //check unsettle bet result available
            RawBetResultLog rawBetResultLog = betResultLogService.checkExists(transactionId, roundId, vendorGameId.toString(), vendorPlayerId.toString());
            if (rawBetResultLog != null) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

}
