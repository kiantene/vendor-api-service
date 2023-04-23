package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import java.util.Map;

import com.nextgen.gameaggregator.entity.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.eventing.events.SettledBetEvent;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.CouchbaseDataIntegrityException;
import com.nextgen.gameaggregator.exception.CurrencyNotSupportedException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.MergedBetDataIntegrityException;
import com.nextgen.gameaggregator.exception.VendorPlatformNotSupportedException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
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
    private ValidationService validationService;

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
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (BetNotFoundException betNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCode.INVALID_REQUEST_PARAMETER); // if there's no value, set it to the default invalid request parameter
                vo.setErrorResponseCode(violation);
            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }          
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (GameNotSupportedException gameNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (VendorPlatformNotSupportedException vendorPlatformNotSupportedException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (DisabledGameException disabledGameException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);

        switch (dto.getGType()) {
            case "0" -> {
                if (dto.getJackpotWin() == null || dto.getJackpotContribute() == null || dto.getHasFreeGame() == null || dto.getHasGamble() == null) {
                    throw new InvalidRequestException();
                }
                break;
            }
            case "7" -> {
                if (dto.getRoomType() == null) {
                    throw new InvalidRequestException();
                }
                break;
            }
            case "9" -> {
                if (dto.getHasBonusGame() == null || dto.getHasGamble() == null) {
                    throw new InvalidRequestException();
                }
                break;
            }
            case "12" -> {
                if (dto.getHasBonusGame() == null) {
                    throw new InvalidRequestException();
                }
                break;
            }
        }
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
     DisabledVendorLineException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
     VendorPlatformNotSupportedException, InvalidRequestException, InvalidPlayerException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(gameSession, dto.getUid());

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlatformCode(), dto.getClientType(), VendorPlatformNotSupportedException::new);

        // Verify game category
        if (!GameCategory.CATEGORY.containsValue(dto.getGType())) throw new InvalidRequestException();
    }
}
