package com.nextgen.gameaggregator.vendor.yesbingo.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Formats;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.GameTypes;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yesbingo.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@Slf4j
public class BetAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;

    public ResponseVo bet(HttpRequestLog httpRequestLog, String traceId, String decryptedData) {

        ResponseVo responseVo = new ResponseVo();

        try {

            /*
            BetDto dto = HttpService.convertJsonToDto(decryptedData, BetDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getUid(), dto.getGameId());

            // Verify data
            this.doVerification(dto, gameSession);

            // Update round id and bet id accordingly based on different game type
            this.setRoundIdAndBetIdByGameType(dto);

            // Process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, httpRequestLog.getRequestBody());
            BigDecimal balance = betEvent.getLastBalance();

            // Set Balance and Currency
            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCEED);

             */
            throw new InvalidRequestException();

        } /*catch (InvalidAgentApiCredentialException | AuthenticationException | InvalidPlayerException |
                 CurrencyNotSupportedException | DisabledAgentPlayerException | DisabledGameException |
                 DisabledVendorLineException | GameNotSupportedException noAuthorizedAccessException) {
            responseVo.setStatus(ResponseCodes.NO_AUTHORIZED_ACCESS);
        } catch (InvalidRequestException | JsonProcessingException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
        } catch (DateTimeParseException dateTimeParseException) {
            responseVo.setStatus(ResponseCodes.WRONG_DATE_SECOND_FORMAT);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setStatus(ResponseCodes.DUPLICATE_TRANSACTIONS);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setStatus(ResponseCodes.CASH_BALANCE_NOT_ENOUGH);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            // 6001-The system is busy (vendor proceeds to cancel the bet)
            responseVo.setStatus(ResponseCodes.SYSTEM_BUSY);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        }*/catch (InvalidRequestException parameterInputErrorException) {
            responseVo.setStatus(ResponseCodes.PARAMETER_INPUT_ERROR);
        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.FAILED);
            httpService.logError(httpRequestLog, exception);
        }

        return responseVo;

    }

    private void doValidation(BetDto dto) throws InvalidRequestException, DateTimeParseException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT);
        formatter.parse(dto.getGameDate());

        // General validation
        ValidationUtils.validateRequest(dto);

        if(dto.getGType() == GameTypes.SLOT && dto.getJackpotContribute() == null) {
            throw new InvalidRequestException();
        } else if(dto.getGType() == GameTypes.BINGO && dto.getPlaySeq() == null) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(BetDto dto, GameSession gameSession)
            throws InvalidPlayerException, CurrencyNotSupportedException, DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify if is valid player
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // Verify currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify Game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);
    }

    private void setRoundIdAndBetIdByGameType(BetDto dto) {
        switch(dto.getGType()) {
            case GameTypes.SLOT -> {
                dto.setRoundId(dto.getGameSeqNo());
                dto.setBetId(dto.getTransferId().toString());
            }
            case GameTypes.BINGO -> {
                dto.setRoundId(dto.getPlaySeq().toString());
                dto.setBetId(dto.getGameSeqNo().toString());
            }
        }
    }
}
