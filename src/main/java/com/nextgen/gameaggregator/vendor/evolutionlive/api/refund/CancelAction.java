package com.nextgen.gameaggregator.vendor.evolutionlive.api.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.ga.writer.RawUnsettledBetRepository;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolutionlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolutionlive.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private RawUnsettledBetRepository rawUnsettledBetRepository;

    @PostMapping(path = EndPoints.CANCEL)
    public ResponseVo cancelAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CancelDto cancelDto = HttpService.convertJsonToDto(body, CancelDto.class);


            // 1. Validate request parameters (Non-database calls)
            this.doValidation(cancelDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(cancelDto.getSid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(String.valueOf(cancelDto.getGame().getDetails().getTable().getId()), gameSession);

            this.doVerification(cancelDto, gameSession);

            // check if cancel bet exist in unsettled bet will not process rollback
//            String mergeId = cancelDto.getTransaction().getRefId() + '_' + cancelDto.getGame().getId() + '_' + gameSession.getVendorGameId() + '_' + gameSession.getVendorPlayerId();
//            UnsettledBet unsettledBet = rawUnsettledBetRepository.findById(mergeId).orElse(null);
//            if (unsettledBet != null) {
//                throw new BetNotFoundException();
//            }

            // 3. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelDto, gameSession, vendorService, httpRequestLog);

//            responseVo.setResponseCode(ResponseCode.BET_ALREADY_EXIST);
            responseVo.setBalance(balance);
            responseVo.setUuid(cancelDto.getUuid());

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_SID);
            httpService.logError(httpRequestLog, e);
        } catch (RecordNotFoundException |
                 BetNotFoundException e) {
            responseVo.setResponseCode(ResponseCode.BET_DOES_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException |
                 InvalidRequestException |
                 GameNotSupportedException |
                 InvalidPlayerException |
                 CurrencyNotSupportedException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 TransactionStillProcessingException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_EXIST);
        } catch (BetResultIdempotentViolationException e) {
            responseVo.setResponseCode(ResponseCode.BET_ALREADY_SETTLED);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private void doValidation(CancelDto cancelDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(cancelDto);
        ValidationUtils.validateRequest(cancelDto.getGame());
        ValidationUtils.validateRequest(cancelDto.getTransaction());
    }

    private void doVerification(CancelDto cancelDto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidPlayerException {

        // 1. Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(gameSession.getToken(), cancelDto.getSid(), AuthenticationException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), cancelDto.getUserId(), InvalidPlayerException::new);
        //ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(cancelDto.getGame().getDetails().getTable().getId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), cancelDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
