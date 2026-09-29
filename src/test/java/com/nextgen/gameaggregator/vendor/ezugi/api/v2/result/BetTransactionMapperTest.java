package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.BetTransaction;
import com.nextgen.gameaggregator.enums.BetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BetTransactionMapperTest {

    @Test
    void mapToBetTransactions_bustItAlongsideOtherBets_classifiesEachBetByItsOwnSideBetType() {
        BetResultRequest request = buildRequestWithBets(
                bet("RegularBet", "500.0"),
                bet("PerfectPair", "50.0"),
                bet("BustIt", "50.0"),
                bet("21+3", "50.0")
        );

        List<BetTransaction> transactions = BetTransactionMapper.mapToBetTransactions(request, "round-1");

        assertThat(transactions).hasSize(4);
        assertThat(betTypeOf(transactions, "RegularBet")).isEqualTo(BetType.NORMAL_BET.code);
        assertThat(betTypeOf(transactions, "PerfectPair")).isEqualTo(BetType.PERFECT_PAIR.code);
        assertThat(betTypeOf(transactions, "BustIt")).isEqualTo(BetType.BUST_IT.code);
        assertThat(betTypeOf(transactions, "21+3")).isEqualTo(BetType.TWENTY_ONE_PLUS_THREE.code);

        BigDecimal totalBetAmount = transactions.stream()
                .map(BetTransaction::getBetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalBetAmount).isEqualByComparingTo("650.0");
    }

    private Integer betTypeOf(List<BetTransaction> transactions, String betName) {
        return transactions.stream()
                .filter(tx -> tx.getExternalTransactionId().endsWith("-" + betName))
                .findFirst()
                .orElseThrow()
                .getBetType();
    }

    private BetResultRequest.Bet bet(String name, String amount) {
        BetResultRequest.Bet bet = new BetResultRequest.Bet();
        bet.setBetName(name);
        bet.setBetAmount(new BigDecimal(amount));
        return bet;
    }

    private BetResultRequest buildRequestWithBets(BetResultRequest.Bet... bets) {
        BetResultRequest request = new BetResultRequest();
        request.setTransactionId("CREDIT-TXN-1");
        request.setDebitTransactionId("DEBIT-TXN-1");
        request.setTimestamp(1234567890L);

        BetResultRequest.GameDataString gameData = new BetResultRequest.GameDataString();
        gameData.setBetsList(List.of(bets));
        gameData.setWinningBets(Map.of());
        request.setGameDataString(gameData);

        return request;
    }
}
