package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.pokerbet.PokerBetService;
import com.nextgen.gameaggregator.vendor.habanero.api.pokerresult.PokerResultService;
import com.nextgen.gameaggregator.vendor.habanero.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.habanero.api.slotbet.SlotBetService;
import com.nextgen.gameaggregator.vendor.habanero.api.slotresult.SlotResultService;
import com.nextgen.gameaggregator.vendor.habanero.constant.*;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TransferAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final SlotBetService slotBetService;
    private final SlotResultService slotResultService;
    private final PokerBetService pokerBetService;
    private final PokerResultService pokerResultService;
    private final RefundService refundService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @Autowired
    public TransferAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          VendorService vendorService,
                          VendorLineService vendorLineService,
                          ValidationService validationService,
                          SlotBetService slotBetService,
                          SlotResultService slotResultService,
                          PokerBetService pokerBetService,
                          PokerResultService pokerResultService,
                          RefundService refundService,
                          RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.slotBetService = slotBetService;
        this.slotResultService = slotResultService;
        this.pokerBetService = pokerBetService;
        this.pokerResultService = pokerResultService;
        this.refundService = refundService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.TRANSFER)
    public ResponseEntity<TransferVo> transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        TransferVo responseVo = new TransferVo();
        int httpStatus = HttpStatus.SC_OK;
        GameSession gameSession = new GameSession();
        TransferDto transferDto = null;
        AtomicBoolean isRequestExists = new AtomicBoolean(false);
        RefundDto refundDto = null;
        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            transferDto = HttpService.convertJsonToDto(body, TransferDto.class);

            //setup debit and credit bet type respond message
            if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
                responseVo.setDebitNCreditMessage();
            }


            //Validate request parameters from vendor (Non-database related)
            this.doValidation(transferDto);

            // get refundDto before check exist
            refundDto = transferDto.getFundTransferRequestDto().getFundDto().getRefundDto();
            // Request idempotent checking for this transaction
            this.doCheckExist(transferDto, isRequestExists, refundDto);

            //Get GameSession
            try {
                gameSession = this.getGameSession(transferDto);
                if (gameSession == null) { //handle if hit null session
                    throw new AuthenticationException();
                }
            } catch (AuthenticationException authenticationException) {
                gameSession = gameSessionService.generateNewSessionToken(transferDto.getFundTransferRequestDto().getAccountId()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, transferDto.getBaseGame().getKeyName());
                gameSessionService.updateByVendorCurrencyCode(gameSession, transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0].getCurrencyCode());
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            //Verify remaining parameters (Verify against database values)
            this.doVerification(transferDto, gameSession);

            //handle transfer action
            if (transferDto.getFundTransferRequestDto().getIsRefund()) {
                //handle refund condition
                responseVo = refundService.refund(transferDto.getFundTransferRequestDto().getFundDto().getRefundDto(), responseVo, gameSession, request);
            } else if (gameSession.getGameCategoryId().equals(Formats.POKER)) {
                //handle Poker condition
                responseVo = this.processPokerTransferAction(responseVo, transferDto, gameSession, request);
            } else {
                //handle Slot condition
                responseVo = this.processSlotTransferAction(responseVo, transferDto, gameSession, request);
            }


        } catch (
                AuthenticationException |
                JsonProcessingException |
                InvalidPlayerException |
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidRequestException |
                NoAvailableLineException |
                CredentialNotFoundException |
                DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                BetNotFoundException |
                InvalidOperatorResponseException generalException
        ) {
            responseVo.setResponseCode(ResponseCodes.TRANSFER_ERROR);
            httpService.logError(httpRequestLog, generalException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setResponseCode(ResponseCodes.INSUFFICIENT_ERROR);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCodes.RETRY_ERROR);
            //return invalid respond 503 to trigger vendor resend when record still in processing
            httpStatus = HttpStatus.SC_SERVICE_UNAVAILABLE;
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.TRANSFER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            //delete request Idempotent log
            this.doCheckExistDelete(transferDto, isRequestExists, refundDto);
            httpService.end(httpRequestLog, responseVo);

        }

        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));

    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.validateRequest(dto.getBaseGame());
        ValidationUtils.validateRequest(dto.getSubAuth());
        ValidationUtils.validateRequest(dto.getFundTransferRequestDto());
        ValidationUtils.validateRequest(dto.getFundTransferRequestDto().getFundDto());
        ValidationUtils.isEquals("fundtransferrequest", dto.getType(), InvalidRequestException::new);
        //date time format validation
        if (!vendorService.isValidDateString(dto.getDtSent())) {
            throw new InvalidRequestException();
        }


    }

    private void doVerification(TransferDto dto, GameSession gameSession) throws
            NoAvailableLineException,
            CredentialNotFoundException {

        //Verify received passkey is the same from credential
        String passkey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSKEY);
        ValidationUtils.isEquals(passkey, dto.getSubAuth().getPasskey(), NoAvailableLineException::new);

        //Verify received brand id is the same from credential
        String brandId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.BRAND_ID);
        ValidationUtils.isEquals(brandId, dto.getSubAuth().getBrandid(), NoAvailableLineException::new);

        //Verify vendor game code is the same from gameSession
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getBaseGame().getKeyName(), NoAvailableLineException::new);

    }

    private void doCheckExist(TransferDto transferDto, AtomicBoolean isRequestExists, RefundDto refundDto) throws TransactionStillProcessingException {

        if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto() != null) {

            // Request idempotent checking for bet and result
            for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                if (requestIdempotentLogService.checkExists(fundInfoDto, transferDto.getFundTransferRequestDto().getAccountId()) == null) {
                    requestIdempotentLogService.create(fundInfoDto, transferDto.getFundTransferRequestDto().getAccountId());
                } else {
                    isRequestExists.set(true);
                    throw new TransactionStillProcessingException("Request still processing.");
                }
            }
        } else {
            // Request idempotent checking for refund
            if (requestIdempotentLogService.checkExists(refundDto, transferDto.getFundTransferRequestDto().getAccountId()) == null) {
                requestIdempotentLogService.create(refundDto, transferDto.getFundTransferRequestDto().getAccountId());
            } else {
                isRequestExists.set(true);
                throw new TransactionStillProcessingException("Request still processing.");
            }
        }
    }

    private void doCheckExistDelete(TransferDto transferDto, AtomicBoolean isRequestExists, RefundDto refundDto) {

        if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto() != null && !isRequestExists.get()) {
            for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                requestIdempotentLogService.delete(fundInfoDto, transferDto.getFundTransferRequestDto().getAccountId());
            }

        } else {
            requestIdempotentLogService.delete(refundDto, transferDto.getFundTransferRequestDto().getAccountId());
        }
    }

    private GameSession getGameSession(TransferDto transferDto) throws AuthenticationException, GameNotSupportedException {
        GameSession gameSession = new GameSession();

        if (transferDto.getFundTransferRequestDto().getIsRetry()) {
            //When isRetry = true, get GameSession by player name and vendor game id
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transferDto.getFundTransferRequestDto().getAccountId(), transferDto.getBaseGame().getKeyName());
        } else if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0].getGameStateMode() != GameStateMode.EXPIRE) {
            //When gamestatemode != 3 get GameSession by token
            gameSession = gameSessionService.verifyToken(transferDto.getFundTransferRequestDto().getToken());
        } else {
            //When gamestatemode = 3 get GameSession by player name and vendor game id, this end request might send out after few days of bet
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transferDto.getFundTransferRequestDto().getAccountId(), transferDto.getBaseGame().getKeyName());
        }
        gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(transferDto.getBaseGame().getKeyName(), gameSession);

        return gameSession;
    }

    private TransferVo processSlotTransferAction(TransferVo responseVo, TransferDto transferDto, GameSession gameSession, HttpServletRequest request) throws
            InvalidRequestException,
            InvalidAgentApiCredentialException,
            BetNotFoundException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            CouchbaseDataIntegrityException,
            MergedBetDataIntegrityException,
            VendorCurrencyNotSupportException,
            NoAvailableLineException,
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException {

        //Loop bet info
        for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
            switch (fundInfoDto.getGameStateMode()) {
                case GameStateMode.STARTROUND ->
                    //process bet result into unsettle bet when gamestatemode = 1(game round start)
                        responseVo = slotBetService.bet(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);

                case GameStateMode.COUTINUEATION, GameStateMode.ENDROUND, GameStateMode.EXPIRE ->
                    //process bet result into settle bet when gamestatemode = 2(game round end/ bonus free spin) or 0(free spin/jackpot) or 3(expire bet round end)
                        responseVo = slotResultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);

                // If the header does not match any of the expected values, return an error response
                default -> {
                    throw new InvalidRequestException();
                }
            }
        }

        return responseVo;
    }

    private TransferVo processPokerTransferAction(TransferVo responseVo, TransferDto transferDto, GameSession gameSession, HttpServletRequest request) throws
            InvalidRequestException,
            InvalidAgentApiCredentialException,
            BetNotFoundException,
            InsufficientBalanceException,
            TransactionStillProcessingException,
            InvalidOperatorResponseException,
            CouchbaseDataIntegrityException,
            MergedBetDataIntegrityException,
            VendorCurrencyNotSupportException,
            NoAvailableLineException,
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException {

        for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
            switch (fundInfoDto.getGameStateMode()) {
                case GameStateMode.STARTROUND ->
                    //process bet result into unsettle bet when gamestatemode = 1(game round start) or 0(double)
                        responseVo = pokerBetService.bet(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);

                case GameStateMode.COUTINUEATION, GameStateMode.ENDROUND, GameStateMode.EXPIRE ->
                    //process bet result into settle bet when gamestatemode = 2(game round end) or 3(expire bet round end)
                        responseVo = pokerResultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);

                // If the header does not match any of the expected values, return an error response
                default -> throw new InvalidRequestException();

            }
        }

        return responseVo;
    }

}
