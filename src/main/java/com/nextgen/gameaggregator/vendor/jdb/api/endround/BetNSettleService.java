package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.GameCategory;
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
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
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
        } catch (InvalidDateException invalidDateException) {
            vo.setResponseCode(ResponseCode.WRONG_DATE_FORMAT);
        } catch (InvalidFormatException invalidFormatException) {
            vo.setResponseCode(ResponseCode.PARAMETER_CANNOT_BE_NEGATIVE);
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
        } catch (GameNotSupportedException gameNotSupportedException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (VendorPlatformNotSupportedException vendorPlatformNotSupportedException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (DisabledGameException disabledGameException) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException, InvalidDateException, InvalidFormatException {
        try {
            ValidationUtils.validateRequest(dto);
        } catch (InvalidRequestException e) {
            // Handle validation errors with dto message
            Map<String, String> validationErrors = e.getValidation();
            for (Map.Entry<String, String> entry : validationErrors.entrySet()) {
                String value = entry.getValue();

                if (value == null) {
                    throw new InvalidRequestException();
                }

                switch (value) {
                    case "WRONG_DATE_FORMAT" -> throw new InvalidDateException();
                    case "PARAMETER_CANNOT_BE_NEGATIVE" -> throw new InvalidFormatException();
                    default -> throw new InvalidRequestException();
                }
            }
        }
        
        switch(dto.getGType()) {
            case "0":
                if (dto.getJackpotWin() == null || dto.getJackpotContribute() == null || dto.getHasFreeGame() == null || dto.getHasGamble() == null){
                    throw new InvalidRequestException();
                }
                break;
            case "7":
                if (dto.getRoomType() == null){
                    throw new InvalidRequestException();
                }
                break;
            case "9":
                if (dto.getHasBonusGame() == null || dto.getHasGamble() == null){
                    throw new InvalidRequestException();
                }
                break;
            case "12":
                if (dto.getHasBonusGame() == null) {
                    throw new InvalidRequestException();
                }
                break;
        }
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
     DisabledVendorLineException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
     VendorPlatformNotSupportedException, InvalidRequestException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlatformCode(), dto.getClientType(), VendorPlatformNotSupportedException::new);

        // Verify game category
        if (!GameCategory.CATEGORY.containsValue(dto.getGType())) throw new InvalidRequestException();
    }
}
