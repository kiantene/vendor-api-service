package com.nextgen.gameaggregator.vendor.whitecliff.api.betresult;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseError;
import com.nextgen.gameaggregator.vendor.whitecliff.service.VendorService;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j

public class CreditAction {
    private final HttpService httpService;
    private final WalletService walletService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final SettledBetService settledBetService;

    @Autowired
    public CreditAction(HttpService httpService, WalletService walletService, GameSessionService gameSessionService, VendorService vendorService, ValidationService validationService, VendorLineService vendorLineService, UnsettledBetCachingService unsettledBetCachingService, AutowireCapableBeanFactory autowireCapableBeanFactory, SettledBetService settledBetService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.settledBetService = settledBetService;
    }

    @PostMapping(path = EndPoints.CREDIT)
    public ResponseVo creditAction(HttpServletRequest request) {

        VendorService vendorService = new VendorService(gameSessionService);
        autowireCapableBeanFactory.autowireBean(vendorService);

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        //Get header for Validation
        String secretKey = request.getHeader("secret-key");

        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // 3. Verify session token
            GameSession gameSession;
            String newToken = (creditDto.getSid() != null) ? creditDto.getSid() : traceId;

            //Validate request parameters (Non-database calls)
            this.doValidation(creditDto);



            BigDecimal balance;

            //If is cancel, process cancel bet
            if(creditDto.getIsCancel() == 1) {
                try {
                    gameSession = gameSessionService.verifyToken(creditDto.getSid());
                    // Check game category to set game code
                    creditDto.setGameCategory(gameSession.getGameCategoryId());
                    this.doVerification(creditDto, gameSession, secretKey);

                } catch (AuthenticationException authenticationException) {
                    UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(creditDto.getRoundId());
                    gameSession = gameSessionService.generateNewSessionTokenByVendorPlayerId(unsettledBet.getVendorPlayerId());
                    gameSessionService.updateByVendorGameCode(gameSession, creditDto.getGameId());
                    gameSessionService.updateByVendorCurrencyId(gameSession);
                    gameSession.setToken(newToken);
                    gameSession.setVendorToken(newToken);
                }
                Integer idempotentCheckAfterSettle = this.settledBetIdempotentCheckCredit(gameSession, creditDto);
                if( idempotentCheckAfterSettle == 1){
                    throw new BetResultIdempotentViolationException();
                }

                balance = walletService.processRollback(traceId,  creditDto, gameSession, vendorService, httpRequestLog);
            }
            //4. Else process normally
            else {
                gameSession = gameSessionService.verifyToken(creditDto.getSid());

                Integer idempotentCheckAfterSettle = this.settledBetIdempotentCheckCredit(gameSession, creditDto);
                if( idempotentCheckAfterSettle == 1){
                    throw new BetResultIdempotentViolationException();
                }

                creditDto.setGameCategory(gameSession.getGameCategoryId());
                this.doVerification(creditDto, gameSession, secretKey);
                vendorService.verifyIsPreProcessingVendorGame(gameSession.getVendorGameId());
                ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(), creditDto.getWinAmount(), creditDto.getJackpotAmount(), false);
                balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);
            }

            responseVo.setBalance(balance);
            responseVo.setStatus(ResponseCodes.SUCCESS);

        } catch (GameNotSupportedException  |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidAgentApiCredentialException |
                 BetNotFoundException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_DEBIT);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.ACCESS_DENIED);
            httpService.logError(httpRequestLog, e);
        }catch (BetResultIdempotentViolationException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.DUPLICATE_CREDIT);
            httpService.logError(httpRequestLog, e);
        }catch (InvalidPlayerException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_USER);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private void doValidation(CreditDto creditDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(creditDto);
        ValidationUtils.validateRequest(creditDto.getGameId());
        ValidationUtils.validateRequest(creditDto.getTxnId());
    }

    private void doVerification(CreditDto creditDto, GameSession gameSession, String secretKey)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException,
            InvalidSignatureException {

        //Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorGameCode()), String.valueOf(creditDto.getGameId()), GameNotSupportedException::new);

        //Validate secret key from header
        String credentialKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(credentialKey,secretKey, InvalidSignatureException::new);

        //Verify UserId
        String vendorToken = String.valueOf(creditDto.getUserId());
        ValidationUtils.isEquals(vendorToken, gameSession.getVendorToken(), InvalidPlayerException::new);

        //Validate Prd_id
        String prdId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PRODUCT_ID);
        ValidationUtils.isEquals(String.valueOf(creditDto.getPrdId()), prdId);

    }

    public Integer settledBetIdempotentCheckCredit(GameSession gameSession, CreditDto dto) {

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        SettledBet settledBet;
        Integer betCheck= 0;

        try {

            settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(vendorPlayerId, dto.getExternalTransactionId());

            if (settledBet != null) { // duplicate request found in settled_bet
                betCheck = 1;
            }
        } catch (BetNotFoundException betNotFoundException) {

        }

        return betCheck;
    }


}

