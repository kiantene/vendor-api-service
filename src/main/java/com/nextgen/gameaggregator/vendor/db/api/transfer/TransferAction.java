package com.nextgen.gameaggregator.vendor.db.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.db.constant.Credentials;
import com.nextgen.gameaggregator.vendor.db.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.db.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.db.constant.TradeType;
import com.nextgen.gameaggregator.vendor.db.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.db.service.VendorService;
import com.nextgen.gameaggregator.vendor.db.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class TransferAction {

    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final ValidationService validationService;
    private final VendorService vendorService;

    @Autowired
    public TransferAction(HttpService httpService,
                          GameSessionService gameSessionService,
                          WalletService walletService,
                          VendorLineService vendorLineService,
                          ValidationService validationService,
                          VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.validationService = validationService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.BALANCE_CHANGE)
    public ResponseVo transfer(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        TransferDto transferDto;
        BigDecimal balance;
        TransferDataVo transferDataVo = new TransferDataVo();
        ResponseVo vo = new ResponseVo();
        CommonDto commonDto;
        try {
            //get body and queryString from vendor request
            String body = httpRequestLog.getRequestBody();
            String queryString = request.getQueryString();

            //convert queryString to dto
            commonDto = HttpService.convertQueryStringToDto(queryString, CommonDto.class);

            //do validation
            ValidationUtils.validateRequest(commonDto);

            String jsonBody = VendorService.decryptToJsonBody(commonDto, vendorLineService, body);
            transferDto = HttpService.convertJsonToDto(jsonBody, TransferDto.class);
            httpRequestLog.setRequestBody(VendorService.
                    getEncryptJsonQueryStringBody(body, jsonBody, queryString));

            //validate request param
            this.doValidation(transferDto);

            // using vendor player username to find gameSession details
            GameSession gameSession = gameSessionService.
                    getGameSessionByVendorPlayerUsername(transferDto.getMemberId());
            //Verification
            this.doVerification(transferDto, gameSession, commonDto);

            switch (transferDto.getTradeType()) {
                case TradeType.BET -> {
                    BetEvent betEvent = walletService.processBet(traceId, gameSession,
                            transferDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                }
                case TradeType.PAYOUT -> {
                    ResultType resultType = getResultType(transferDto, gameSession);
                    balance = walletService.processBetResult(traceId, gameSession,
                            transferDto, resultType, vendorService, httpRequestLog);
                }
                default -> throw new InvalidRequestException();
            }
            vo.setResponseCode(ResponseCodes.SUCCESS);

            transferDataVo.setBalance(balance.toBigInteger());
            transferDataVo.setTradeType(transferDto.getTradeType());
            transferDataVo.setTradeAmount(transferDto.getTradeAmount().toBigInteger());
            vo.setData(transferDataVo);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.PLAYER_NOT_EXIST);

        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.INVALID_GAME_ID);

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);

        } catch (BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.BET_NOT_FOUND);

        } catch (CredentialNotFoundException | CurrencyNotSupportedException | JsonProcessingException |
                 UnexpectedTypeException | InvalidPlayerException | InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCodes.INVALID_PARAMETER);

        } catch (InvalidSignatureException exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCode(ResponseCodes.INVALID_SIGNATURE);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR);
        } finally {
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // Validation with custom exception
        ValidationUtils.validateRequest(dto);

    }

    private void doVerification(TransferDto dto, GameSession gameSession, CommonDto commonDto) throws
            InvalidPlayerException, DisabledVendorLineException, CurrencyNotSupportedException, CredentialNotFoundException,
            AuthenticationException, DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException {

        if (dto.getTradeType() == TradeType.BET) {
            //validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, dto.getMemberId());
        }

        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        VendorService.verifyHash(commonDto.getAgent(), commonDto.getTimestamp(), secretKey, commonDto.getSign());
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getMemberId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), DisabledGameException::new);

    }

    private ResultType getResultType(TransferDto dto, GameSession gameSession) {
        ResultType resultType = ResultType.BET; // Default value is bet

        //Check If is an unsettled bet is found,then settle that bet with result type win
        boolean isUnsettled = vendorService.searchUnsettledBetForSettle(dto, gameSession);

        if (dto.getTradeType() == TradeType.PAYOUT && dto.getTradeAmount().compareTo(BigDecimal.ZERO) > 0) {

            if (isUnsettled) {
                //If got any roundId then use win for settle this bet
                resultType = ResultType.WIN;
            } else {
                resultType = ResultType.BET_WIN;
            }

        } else if (dto.getTradeType() == TradeType.PAYOUT) {

            if (isUnsettled) {
                resultType = ResultType.END;
            } else {
                resultType = ResultType.BET_LOSE;
            }

        }
        return resultType;

    }

}
