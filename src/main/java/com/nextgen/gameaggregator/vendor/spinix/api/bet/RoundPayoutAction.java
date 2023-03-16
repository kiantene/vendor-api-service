package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RoundPayoutAction {

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
    private BetHistoryService betHistoryService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @PostMapping(path = EndPoints.ROUND)
    public RoundPayoutVo bet(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();
        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();

        try {

            // Convert original request body into dto
            RoundPayoutDto dto = HttpService.convertJsonToDto(body, RoundPayoutDto.class);
            ValidationUtils.validateRequest(dto);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Gather require data
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(dto.getUserId());
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(dto.getGameId(), vendorPlayer.getVendorId());

            // Get game session
//            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(vendorPlayer.getUsername(), vendorGame.getVendorGameCode());

            List<RoundPayoutTransactionDto> list = dto.getTransactionList();
            RoundPayoutTransactionDto cancelBet = RoundPayoutDto.findTransaction(list, "cancelBet");
            RoundPayoutTransactionDto betRecord = RoundPayoutDto.findTransaction(list, "bet");
            RoundPayoutTransactionDto winRecord = RoundPayoutDto.findTransaction(list, "win");

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // MD5 body request as external transaction id for both bet and win record
            VendorService vendorService = new VendorService();
            String externalTransactionId = vendorService.md5(body);

            RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();

            // Set necessary values to process bet record
            if(betRecord != null && betRecord.getType().equals("bet")) {
                BetDto betDto = new ObjectMapper().convertValue(dto, BetDto.class);
                betDto.setExternalTransactionId(externalTransactionId);
                betDto.setAmount(betRecord.getAmount().abs());
                betDto.setTimestamp(betRecord.getTimestamp());
                BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

                // Set Balance
                roundPayoutDataWalletVo.setBalance(betEvent.getLastBalance());
            }

            // Check if bet record exists before process win record
            BetHistory betHistory = betHistoryService.getBetTransactionByRoundId(dto.getRoundId(), vendorGame.getId(), vendorPlayer.getId());

            // Set necessary values to process win record
            if (betHistory.getRoundId() != null) {
                WinDto winDto = new ObjectMapper().convertValue(dto, WinDto.class);
                winDto.setExternalTransactionId(externalTransactionId);
                winDto.setAmount(winRecord.getAmount());
                winDto.setWinType(this.getWinType(winRecord));
                winDto.setTimestamp(winRecord.getTimestamp());
                winDto.setEffectiveTurnover(dto.getValidTurnover().abs());

                BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDto, body);

                // Emit event for additional asynchronous processing
                if(winRecord.getIsEnd() == true) {
                    Thread.sleep(1000); // Sleep for 1 second
                    EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));
                }

                // Set Balance
                roundPayoutDataWalletVo.setBalance(betResultEvent.getLastBalance());
            }

            // Set Currency + RoundPayoutDataWalletVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(HttpStatus.SC_OK);

        } catch(BetNotFoundException e) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch(DuplicateExternalTransactionIdException e) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        } catch (GameNotSupportedException e) {
            throw new RuntimeException(e);
        } catch (InsufficientBalanceException e) {
            throw new RuntimeException(e);
        } catch (InvalidOperatorResponseException e) {
            throw new RuntimeException(e);
        } catch (DisabledVendorLineException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvalidAgentApiCredentialException e) {
            throw new RuntimeException(e);
        } catch (BetResultNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvalidPlayerException e) {
            throw new RuntimeException(e);
        } catch (DisabledAgentPlayerException e) {
            throw new RuntimeException(e);
        } catch (DisabledGameException e) {
            throw new RuntimeException(e);
        } catch (InvalidRequestException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch(Exception e) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } finally {
            if(roundPayoutVo.getStatus() == HttpStatus.SC_OK) {
                roundPayoutVo.setData(roundPayoutDataVo);
            } else {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
            httpService.end(httpRequestLog, roundPayoutVo);
        }

        return roundPayoutVo;
    }

    private void doValidation(RoundPayoutDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RoundPayoutDto dto, GameSession gameSession)
            throws InvalidPlayerException, AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        // Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private WinType getWinType(RoundPayoutTransactionDto winRecord) {
        WinType winType;
        if (winRecord.getInfo().equals("feature_freespin")) {
            winType = WinType.WIN;
        } else {
            winType = (winRecord.getAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
        }

        return winType;
    }
}