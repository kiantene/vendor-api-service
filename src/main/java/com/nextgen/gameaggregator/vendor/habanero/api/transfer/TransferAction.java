package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.pokerbet.PokerBetService;
import com.nextgen.gameaggregator.vendor.habanero.api.pokerresult.PokerResultService;
import com.nextgen.gameaggregator.vendor.habanero.api.slotbet.SlotBetService;
import com.nextgen.gameaggregator.vendor.habanero.api.refund.RefundService;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TransferAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private SlotBetService slotBetService;
    @Autowired
    private SlotResultService slotResultService;
    @Autowired
    private PokerBetService pokerBetService;
    @Autowired
    private PokerResultService pokerResultService;
    @Autowired
    private RefundService refundService;

    @PostMapping(path = EndPoints.TRANSFER)
    public ResponseEntity<TransferVo> transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Construct VO
        TransferVo responseVo = new TransferVo();
        Integer httpStatus = HttpStatus.SC_OK;

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            TransferDto transferDto = HttpService.convertJsonToDto(body, TransferDto.class);

            //setup debit and credit bet type respond message
            if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
                responseVo.setDebitNCreditMessage();
            }

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(transferDto);

            //Get GameSession
            GameSession gameSession = this.getGameSession(transferDto);

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
            //return invalid respond to trigger vendor resend when record still in processing
            responseVo.setResponseCode(ResponseCodes.RETRY_ERROR);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (Exception exception) {
            responseVo.setResponseCode(ResponseCodes.TRANSFER_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);

        }

        if (responseVo.getFundTransferResponseVo().getStatusVo().getRetryStatus() != null) {
            //return invalid respond 404 to trigger vendor resend when record still in processing
            responseVo = null;
            httpStatus = HttpStatus.SC_NOT_FOUND;
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

    private GameSession getGameSession(TransferDto transferDto) throws AuthenticationException {
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
                case GameStateMode.STARTROUND -> {
                    //process bet result into unsettle bet when gamestatemode = 1(game round start)
                    responseVo = slotBetService.bet(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);
                }
                case GameStateMode.COUTINUEATION, GameStateMode.ENDROUND, GameStateMode.EXPIRE -> {
                    //process bet result into settle bet when gamestatemode = 2(game round end/ bonus free spin) or 0(free spin/jackpot) or 3(expire bet round end)
                    responseVo = slotResultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);
                }
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

        FundInfoDto fundInfoDto = transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0];
        switch (fundInfoDto.getGameStateMode()) {
            case GameStateMode.STARTROUND, GameStateMode.COUTINUEATION -> {
                //process bet result into unsettle bet when gamestatemode = 1(game round start) or 0(double)
                responseVo = pokerBetService.bet(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);
            }
            case GameStateMode.ENDROUND, GameStateMode.EXPIRE -> {
                //process bet result into settle bet when gamestatemode = 2(game round end) or 3(expire bet round end)
                responseVo = pokerResultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, request);
            }
            // If the header does not match any of the expected values, return an error response
            default -> {
                throw new InvalidRequestException();
            }
        }

        return responseVo;
    }

}
