package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientApiRequest;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.common.OperatorApiCallerV2;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.couchbase.RoundTxn;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BetRollbackProcessor {
    private final GameRoundService gameRoundService;
    private final ClientRequestService clientRequestService;
    private final OperatorApiCallerV2 operatorApiCaller;

    public PlayerBalanceData process(BetRollbackContext context, GameTransaction txn) {
        String docId = txn.getRoundDocId();
        Optional<GameRound> roundOpt = gameRoundService.get(docId);

        if (roundOpt.isEmpty()) {
            // throw not found
            return defaultBalanceData(context, "");
        }

        GameRound round = roundOpt.get();
        List<RoundTxn> txnList = round.getTransactions();

        txnList.stream()
                .filter(t -> (t.getType() == TxnType.BET || t.getType() == TxnType.RESULT))
                .filter(t -> t.getStatus() == TxnStatus.SUCCESS)
                .forEach(t -> callToOperator(context, round, t));

        // TODO: update status to rollback
        return defaultBalanceData(context, round.getCurrency());
    }

    private PlayerBalanceData defaultBalanceData(BetRollbackContext context, String currency) {
        return PlayerBalanceData.getDefault(
                context.getTraceId(),
                context.getVendorPlayerUsername(),
                currency
        );
    }

    private PlayerBalanceData callToOperator(BetRollbackContext context, GameRound round, RoundTxn txn) {
        try {
            ClientApiRequest<WalletRollbackDto> apiRequest = clientRequestService.createClientApiRequest(
                    round.getAgentMeta().getAgentId(),
                    EndPoints.WALLET_ROLLBACK,
                    mapToClientRequest(context, round, txn)
            );

            ClientBalanceResponse response = operatorApiCaller.post(
                    apiRequest.getBaseUrl(),
                    apiRequest.getPath(),
                    apiRequest.getHeaders(),
                    apiRequest.getRequestObject()
            );

            return response.getData();
        } catch (Exception ex) {

            throw ex;
        }
    }

    private WalletRollbackDto mapToClientRequest(BetRollbackContext context, GameRound round, RoundTxn txn) {
        WalletRollbackDto dto = new WalletRollbackDto();

        dto.setTraceId(context.getTraceId());
        dto.setTransactionId(UuidUtil.newUuidV7String());
        dto.setBetId(txn.getGaBetId());
        dto.setExternalTransactionId(context.getIdempotencyKey());
        dto.setRoundId(round.getRoundId());
        dto.setGameCode(round.getAgentMeta().getGameCode());
        dto.setUsername(round.getAgentMeta().getUsername());
        dto.setCurrency(round.getAgentMeta().getCurrency());
        dto.setTimestamp(context.getTimestamp());

        return dto;
    }
}
