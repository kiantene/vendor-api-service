package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.whitecliff.api.bet.DebitDto;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseError;
import com.nextgen.gameaggregator.vendor.whitecliff.service.VendorService;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
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
public class BonusAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private SettledBetService settledBetService;

    @Autowired
    public BonusAction(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, ValidationService validationService, VendorService vendorService, VendorLineService vendorLineService, SettledBetService settledBetService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.settledBetService = settledBetService;
    }

    @PostMapping(path = EndPoints.BONUS)
    public ResponseVo bonusAction(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        VendorService vendorService = new VendorService(gameSessionService);


        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        //Get header for Validation
        String secretKey = request.getHeader("secret-key");

        try {
            //Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BonusDto bonusDto = HttpService.convertJsonToDto(body, BonusDto.class);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(bonusDto.getSid());

            // Validate request parameters (Non-database calls)
            this.doValidation(bonusDto);

            // Check game category to set game code
            bonusDto.setGameCategory(gameSession.getGameCategoryId());

            this.doVerification(bonusDto, gameSession, secretKey);

            Integer idempotentCheckAfterSettle = this.settledBetIdempotentCheckBonus(gameSession, bonusDto);
            if( idempotentCheckAfterSettle == 1){
                throw new BetResultIdempotentViolationException();
            }

            BigDecimal balance;

            balance = walletService.processBetResult(traceId, gameSession, bonusDto, ResultType.BET_WIN, vendorService, httpRequestLog);
            responseVo.setBalance(balance);

            responseVo.setStatus(ResponseCodes.SUCCESS);

        } catch (GameNotSupportedException  |
                 DisabledVendorLineException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidAgentApiCredentialException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_DEBIT);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.DUPLICATE_BONUS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.ACCESS_DENIED);
            httpService.logError(httpRequestLog, e);
        }catch (InvalidPlayerException e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.INVALID_USER);
            httpService.logError(httpRequestLog, e);
        }catch (Exception e) {
            responseVo.setStatus(ResponseCodes.FAILED);
            responseVo.setError(ResponseError.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;

    }

    private void doValidation(BonusDto bonusDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(bonusDto);
    }

    private void doVerification(BonusDto bonusDto, GameSession gameSession, String secretKey)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidRequestException,
            GameNotSupportedException,
            InvalidSignatureException {

        // 1. Verify Username, GameCode, CurrencyCode
        ValidationUtils.isEquals(String.valueOf(gameSession.getVendorToken()), String.valueOf(bonusDto.getUserId()), InvalidPlayerException::new);

        // 2. Validate secret key from header
        String credentialKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(credentialKey,secretKey, InvalidSignatureException::new);

        //Validate Prd_id
        String prdId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PRODUCT_ID);
        ValidationUtils.isEquals(String.valueOf(bonusDto.getPrdId()), prdId);

    }


    public Integer settledBetIdempotentCheckBonus(GameSession gameSession, BonusDto dto) {

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
