package com.nextgen.gameaggregator.vendor.amusnet.api.betnsettle;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetNSettleAction {


    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;

    @Autowired
    public BetNSettleAction(HttpService httpService,
                            GameSessionService gameSessionService,
                            WalletService walletService,
                            ValidationService validationService,
                            VendorService vendorService,
                            VendorLineService vendorLineService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
    }

    @PostMapping(path = EndPoints.WITHDRAW_AND_DEPOSIT)
    public String betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        BetNSettleVo vo = new BetNSettleVo();
        XmlMapper xmlMapper = new XmlMapper();
        String traceId = httpRequestLog.getId();

        try {
            String body = httpRequestLog.getRequestBody();
            // Retrieve request body in original string format and convert into dto
            BetNSettleDto betNSettleDto = xmlMapper.readValue(body, BetNSettleDto.class);

            // Validate request parameters from vendor (Non-database calls)
            this.doValidation(betNSettleDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betNSettleDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betNSettleDto.getVendorGameId(), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            // Send win result to Operator
            ResultType resultType = getResultType(betNSettleDto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betNSettleDto, resultType, vendorService, httpRequestLog);

            // Set response data
            vo.setBalance(balance.toBigInteger());
            vo.setCasinoTransferId(traceId);
            vo.setResponseCodes(ResponseCodes.OK);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.DUPLICATE);

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INSUFFICIENT_FUNDS);

        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(Status.SC_INSUFFICIENT_FUNDS.code)) {
                httpService.logError(httpRequestLog, e);
                vo.setResponseCodes(ResponseCodes.INSUFFICIENT_FUNDS);
            } else {
                httpService.logError(httpRequestLog, e);
                vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            }
        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.TIME_OUT);

        } catch (Exception e) { // any other exception encountered
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);

        } finally {
            vendorService.buildResponseVo(vo);
            httpService.end(httpRequestLog, vo);
        }
        return vo.getResponseXMLFormat();
    }

    private void doValidation(BetNSettleDto betNSettleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betNSettleDto);
    }

    private void doVerification(BetNSettleDto betNSettleDto, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidPlayerException,
            InvalidCredentialsException,
            CurrencyNotSupportedException {

        ///validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betNSettleDto.getPlayerId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betNSettleDto.getCurrency(), CurrencyNotSupportedException::new);
        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), betNSettleDto.getPlayerId(), AuthenticationException::new);

        String userName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSWORD);
        String portalCodeEQ = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE_EQ);
        String portalCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE);
        String categoryCodeList = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CATEGORY_CODE_EQ);
        String verifiedPortalCode = vendorService.checkGameCodeIsOpenEQGame(categoryCodeList, betNSettleDto.getVendorGameId(), portalCodeEQ, portalCode);

        ValidationUtils.isEquals(userName, betNSettleDto.getUserName(), InvalidCredentialsException::new);
        ValidationUtils.isEquals(password, betNSettleDto.getPassword());
        ValidationUtils.isEquals(verifiedPortalCode, betNSettleDto.getPortalCode());
    }

    private ResultType getResultType(BetNSettleDto betNSettleDto) {
        ResultType resultType = ResultType.BET_LOSE;

        if (betNSettleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }

}
