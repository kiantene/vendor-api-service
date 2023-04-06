package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.MergedBetDataIntegrityException;
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
public class BetNSettleService {

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

    public CommonVo betNSettle(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BetNSettleDto betNSettleDto = HttpService.convertJsonToDto(actionDto.getParams(), BetNSettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betNSettleDto.getUid(), betNSettleDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 4.3 Process Bet Result and End Round
            SettledBetEvent betResultEvent = walletService.processUnsettleResultSettle(traceId, gameSession, betNSettleDto, actionDto.getParams());
            vo.setBalance(betResultEvent.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            vo.setResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (BetNotFoundException betNotFoundException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidRequestException invalidRequestException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException disabledGameException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
     DisabledVendorLineException, DisabledGameException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
