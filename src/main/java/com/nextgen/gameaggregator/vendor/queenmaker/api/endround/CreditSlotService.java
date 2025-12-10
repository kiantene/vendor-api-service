package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Txtype;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class CreditSlotService {
    private final VendorLineService vendorLineService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final SettledBetService settledBetService;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CreditSlotService(

            VendorLineService vendorLineService,
            WalletService walletService,
            VendorService vendorService,
            SettledBetService settledBetService,
            UnsettledBetCachingService unsettledBetCachingService, RequestIdempotentLogService requestIdempotentLogService) {


        this.vendorLineService = vendorLineService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.settledBetService = settledBetService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public TransactionsVo slotBet(HttpRequestLog httpRequestLog,
                                  String clientId,
                                  String clientSecret,
                                  String traceId,
                                  String body,
                                  GameSession gameSession) throws
            JsonProcessingException,
            InvalidRequestException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            TransactionStillProcessingException,
            BetNotFoundException,
            CredentialNotFoundException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            InvalidOperatorResponseException,
            InternalServerTimeoutRetryException,
            RecordNotFoundException,
            BetRefundIdempotentViolationException,
            InvalidFormatException {

        CreditSlotDto creditSlotDto = HttpService.convertJsonToDto(body,
                CreditSlotDto.class);

        // 1. Validate request parameters (Non-database calls)
        this.doValidation(creditSlotDto);

        CreditSlotTransactionsDto transaction = creditSlotDto.getTransactions().get(0);

        return this.processData(transaction,
                clientId,
                clientSecret,
                httpRequestLog,
                traceId,
                gameSession);
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditSlotTransactionsDto creditSlotTransactionsDto,
                                GameSession gameSession,
                                String clientId,
                                String clientSecret)
            throws
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            InvalidRequestException {

        // 1. Validate Credentials
        String vendorClientId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(),
                Credentials.CLIENT_ID);
        String vendorClientSecret = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(),
                Credentials.CLIENT_SECRET);
        ValidationUtils.isEquals(clientId,
                vendorClientId,
                InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret,
                vendorClientSecret,
                InvalidVendorLineException::new);

        // 2. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = VendorService.splitGameCode(gameSession.getVendorGameCode(),
                2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(creditSlotTransactionsDto.getGpcode(),
                gpcode,
                GameNotSupportedException::new);
        ValidationUtils.isEquals(creditSlotTransactionsDto.getGamecode(),
                gamecode,
                GameNotSupportedException::new);
        ValidationUtils.isEquals(creditSlotTransactionsDto.getCur(),
                gameSession.getVendorCurrencyCode(),
                CurrencyNotSupportedException::new);
        // 3. Validate TxType is exist
        if (!Txtype.txtTypeList.contains(creditSlotTransactionsDto.getTxtype())) {
            throw new InvalidRequestException();
        }
    }

    private TransactionsVo processData(CreditSlotTransactionsDto creditSlotTransactionsDto,
                                       String clientId,
                                       String clientSecret,
                                       HttpRequestLog httpRequestLog,
                                       String traceId,
                                       GameSession gameSession) throws
            GameNotSupportedException,
            InvalidRequestException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            TransactionStillProcessingException,
            BetNotFoundException,
            CredentialNotFoundException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException,
            BetResultIdempotentViolationException,
            MergedBetDataIntegrityException,
            InsufficientBalanceException,
            InvalidOperatorResponseException,
            InternalServerTimeoutRetryException,
            RecordNotFoundException,
            BetRefundIdempotentViolationException,
            InvalidFormatException {
        
        boolean isRequestExists = false;
        TransactionsVo transactionsVo = new TransactionsVo();
        BigDecimal balance;

        try {
            this.doValidation(creditSlotTransactionsDto);

            if (requestIdempotentLogService.checkExists(creditSlotTransactionsDto, creditSlotTransactionsDto.getUserid()) == null) {
                requestIdempotentLogService.create(creditSlotTransactionsDto, creditSlotTransactionsDto.getUserid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }
            this.doVerification(creditSlotTransactionsDto,
                    gameSession,
                    clientId,
                    clientSecret);

            if (creditSlotTransactionsDto.getTxtype().equals(Txtype.CANCEL_BET)) {
                RollbackTransactionDto rollbackTransactionDto = new ModelMapper().map(creditSlotTransactionsDto,
                        RollbackTransactionDto.class);
                balance = walletService.processRollback(traceId,
                        rollbackTransactionDto,
                        gameSession,
                        vendorService,
                        httpRequestLog);
            } else {
                this.settledBetIdempotentCheck(gameSession,
                        creditSlotTransactionsDto);
                this.unSettleBetCheck(creditSlotTransactionsDto);
                ResultType resultType = vendorService.calculateResultType(creditSlotTransactionsDto.getBetAmount(),
                        creditSlotTransactionsDto.getWinAmount(),
                        creditSlotTransactionsDto.getJackpotAmount(),
                        true,
                        creditSlotTransactionsDto.getBetStatus());
                balance = walletService.processBetResult(traceId,
                        gameSession,
                        creditSlotTransactionsDto,
                        resultType,
                        vendorService,
                        httpRequestLog);
            }
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(creditSlotTransactionsDto.getPtxid());
            transactionsVo.setBal(balance);
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());
            transactionsVo.setDup(false);

            return transactionsVo;

        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(creditSlotTransactionsDto, creditSlotTransactionsDto.getUserid());
            }
        }
    }

    private void settledBetIdempotentCheck(GameSession gameSession, CreditSlotTransactionsDto creditSlotTransactionsDto)
            throws BetResultIdempotentViolationException {

        Long vendorPlayerId = gameSession.getVendorPlayerId();
        try {
            SettledBet settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(
                    vendorPlayerId, creditSlotTransactionsDto.getExternalTransactionId()
            );

            if (settledBet != null) {
                throw new BetResultIdempotentViolationException();
            }
        } catch (BetNotFoundException e) {
        }
    }

    //To do 23/6/2025
    private void unSettleBetCheck(CreditSlotTransactionsDto creditSlotTransactionsDto)
            throws BetNotFoundException {

        UnsettledBet unsettledBet = unsettledBetCachingService.getTop1UnsettledBetWithRoundId(
                creditSlotTransactionsDto.getExternalroundid()
        );

        if (unsettledBet == null) {
            throw new BetNotFoundException("Unsettled bet not found for roundId: " + creditSlotTransactionsDto.getExternalroundid());
        }
    }
}
