package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.habanero.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.habanero.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.habanero.api.result.ResultService;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.constant.GameStateMode;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
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
    private BetService betService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private RefundService refundService;

    @PostMapping(path = EndPoints.TRANSFER)
    public ResponseEntity<TransferVo> transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct VO
        TransferVo responseVo = new TransferVo();
        FundTransferResponseVo fundTransferResponseVo = new FundTransferResponseVo();
        StatusVo statusVo = new StatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);
        Integer httpStatus = HttpStatus.SC_OK;

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into authDto
            TransferDto transferDto = HttpService.convertJsonToDto(body, TransferDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(transferDto);

            //Get GameSession
            GameSession gameSession = this.getGameSession(transferDto);

            //Verify remaining parameters (Verify against database values)
            this.doVerification(transferDto, gameSession);

            //handle transfer action
            responseVo = this.processTransferAction(transferDto, gameSession, traceId, httpRequestLog, body);

        } catch (
                AuthenticationException |
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
        } catch (Exception exception) {
            statusVo.setSuccess(false);
            statusVo.setAuthError(true);
            statusVo.setMessage(ResponseCodes.TRANSFER_FAIL);
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

    private GameSession getGameSession(TransferDto transferDto) throws AuthenticationException {
        GameSession gameSession = new GameSession();
        if (!transferDto.getFundTransferRequestDto().getIsRetry()) {
            //check 1st fundinfo gamestatemode value
            if (transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0].getGameStateMode() != GameStateMode.EXPIRE) {
                //Get GameSession by token
                gameSession = gameSessionService.verifyToken(transferDto.getFundTransferRequestDto().getToken());
            } else {
                //When gamestatemode = 3 get GameSession by player name and vendor game id, this end request might send out after few days of bet
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transferDto.getFundTransferRequestDto().getAccountId(), transferDto.getBaseGame().getKeyName());
            }
        } else {
            //When isRetry = true, get GameSession by player name and vendor game id
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(transferDto.getFundTransferRequestDto().getAccountId(), transferDto.getBaseGame().getKeyName());
        }

        return gameSession;
    }

    private TransferVo processTransferAction(TransferDto transferDto, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog, String body) {
        TransferVo responseVo = new TransferVo();
        FundTransferResponseVo fundTransferResponseVo = new FundTransferResponseVo();
        StatusVo statusVo = new StatusVo();
        fundTransferResponseVo.setStatusVo(statusVo);
        responseVo.setFundTransferResponseVo(fundTransferResponseVo);

        //setup debit and credit bet type respond message
        if (transferDto.getFundTransferRequestDto().getFundDto().getDebitAndCredit()) {
            statusVo.setSuccessDebit(false);
            statusVo.setSuccessCredit(false);
        }

        //check not refund/recredit transfer
        if (!transferDto.getFundTransferRequestDto().getIsRetry()) {
            //Loop bet info
            for (FundInfoDto fundInfoDto : transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()) {
                if (fundInfoDto.getGameStateMode() == GameStateMode.BET) {
                    //process bet result into unsettle bet when gamestatemode = 1(game round start)
                    responseVo = betService.bet(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), gameSession, traceId, body, httpRequestLog);
                    if (!responseVo.getFundTransferResponseVo().getStatusVo().getSuccess()) {
                        //stop loop and return error respond when debit and credit condition
                        break;
                    }
                } else {
                    //process bet result into settle bet when gamestatemode = 2(game round end/ bonus free spin) or 0(free spin/jackpot) or 3(expire bet round end)
                    responseVo = resultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), fundInfoDto.getGameStateMode(), gameSession, traceId, httpRequestLog);
                }
            }
        } else {
            //handle refund and recredit
            if (transferDto.getFundTransferRequestDto().getIsRecredit()) {
                //handle recredit condition
                //get 1st array object as record
                FundInfoDto fundInfoDto = transferDto.getFundTransferRequestDto().getFundDto().getFundInfoDto()[0];

                //handle recredit action
                responseVo = resultService.result(fundInfoDto, transferDto.getFundTransferRequestDto(), responseVo, transferDto.getBaseGame().getKeyName(), fundInfoDto.getGameStateMode(), gameSession, traceId, httpRequestLog);

            } else {
                //handle refund condition
                //handle refund action
                responseVo = refundService.refund(transferDto.getFundTransferRequestDto().getFundDto().getRefundDto(), responseVo, gameSession, traceId, httpRequestLog);
            }
        }

        return responseVo;
    }

}
