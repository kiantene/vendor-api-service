package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetEvent;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.AgentPlayerService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BetService {

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

    public CommonVo bet(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BetDto betDto = HttpService.convertJsonToDto(actionDto.getParams(), BetDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getUid(), betDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 4.3 Process Bet Request
            //BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, actionDto.getParams());
            UnsettledBetEvent betEvent = walletService.processUnsettledBet(traceId, gameSession, betDto, actionDto.getParams());

            vo.setBalance(betEvent.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);
        
        } catch (InsufficientBalanceException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (CouchbaseDataIntegrityException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidOperatorResponseException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidAgentApiCredentialException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidRequestException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException{
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws DisabledVendorLineException, 
    DisabledAgentPlayerException, DisabledGameException{
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
