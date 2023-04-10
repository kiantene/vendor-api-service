package com.nextgen.gameaggregator.vendor.jdb.api.result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SettleService {

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

    public CommonVo settle(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            SettleDto settleDto = HttpService.convertJsonToDto(actionDto.getParams(), SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleDto.getUid(), settleDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            SettledBetEvent betResultEvent = walletService.processResultSettle(traceId, gameSession, settleDto, actionDto.getParams());
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
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
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

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
    DisabledVendorLineException, DisabledGameException {
       // Verify vendor line is active
       vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

       // Verify agent player is active
       agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

       // Verify vendor game is active
       vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
   }
}
