package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class CurrencyConversionService {

    public void doCurrencyConversionRateFromVendorForBetResult(WalletBetResultDto walletBetResultDto, BigDecimal conversionRate) {

        //betResult consists of 5 type of amount need to do conversion when they are not 0 or null.
        //betAmount, winAmount ,effectiveTurnover ,winLoss ,jackpotAmount;

        if (walletBetResultDto.getBetAmount() != BigDecimal.ZERO && walletBetResultDto.getBetAmount() != null) {
            walletBetResultDto.setBetAmount(stripZeroToString(walletBetResultDto.getBetAmount().multiply(conversionRate)));
        }

        if (walletBetResultDto.getWinAmount() != BigDecimal.ZERO && walletBetResultDto.getWinAmount() != null) {
            walletBetResultDto.setWinAmount(stripZeroToString(walletBetResultDto.getWinAmount().multiply(conversionRate)));
        }

        if (walletBetResultDto.getEffectiveTurnover() != BigDecimal.ZERO && walletBetResultDto.getEffectiveTurnover() != null) {
            walletBetResultDto.setEffectiveTurnover(stripZeroToString(walletBetResultDto.getEffectiveTurnover().multiply(conversionRate)));
        }

        if (walletBetResultDto.getWinLoss() != BigDecimal.ZERO && walletBetResultDto.getWinLoss() != null) {
            walletBetResultDto.setWinLoss(stripZeroToString(walletBetResultDto.getWinLoss().multiply(conversionRate)));
        }

        if (walletBetResultDto.getJackpotAmount() != BigDecimal.ZERO && walletBetResultDto.getJackpotAmount() != null) {
            walletBetResultDto.setJackpotAmount(stripZeroToString(walletBetResultDto.getJackpotAmount().multiply(conversionRate)));
        }
    }

    public void doCurrencyConversionRateFromVendorForBetHistoryBeforeSendToKafka(BetHistory betHistory, BigDecimal conversionRate) {

        //betResult consists of 5 type of amount need to do conversion when they are not 0 or null.
        //betAmount, winAmount ,effectiveTurnover ,winLoss ,jackpotAmount;

        if (betHistory.getBetAmount() != BigDecimal.ZERO && betHistory.getBetAmount() != null) {
            betHistory.setBetAmount(stripZeroToString(betHistory.getBetAmount().multiply(conversionRate)));
        }

        if (betHistory.getWinAmount() != BigDecimal.ZERO && betHistory.getWinAmount() != null) {
            betHistory.setWinAmount(stripZeroToString(betHistory.getWinAmount().multiply(conversionRate)));
        }

        if (betHistory.getEffectiveTurnover() != BigDecimal.ZERO && betHistory.getEffectiveTurnover() != null) {
            betHistory.setEffectiveTurnover(stripZeroToString(betHistory.getEffectiveTurnover().multiply(conversionRate)));
        }

        if (betHistory.getWinLoss() != BigDecimal.ZERO && betHistory.getWinLoss() != null) {
            betHistory.setWinLoss(stripZeroToString(betHistory.getWinLoss().multiply(conversionRate)));
        }

        if (betHistory.getJackpotAmount() != BigDecimal.ZERO && betHistory.getJackpotAmount() != null) {
            betHistory.setJackpotAmount(stripZeroToString(betHistory.getJackpotAmount().multiply(conversionRate)));
        }
    }

    // deprecated, use convertFromVendorRate instead
    public BigDecimal doCurrencyConversionRateFromVendorForAmount(BigDecimal amount, BigDecimal conversionRate) {

        //amount that needed to do conversion when they are not 0 or null.
        if (amount != BigDecimal.ZERO && amount != null) {
            amount = (stripZeroToString(amount.multiply(conversionRate)));
        }

        return amount;

    }

    public static BigDecimal convertFromVendorRate(BigDecimal amount, BigDecimal rate, boolean checkPositive) {
        BigDecimal convertedAmount = amount;
        if (amount != null) { // only convert if it is not null
            if (checkPositive) {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    convertedAmount = new BigDecimal(amount.multiply(rate).stripTrailingZeros().toPlainString());
                }
            } else {
                convertedAmount = new BigDecimal(amount.multiply(rate).stripTrailingZeros().toPlainString());
            }
        }
        return convertedAmount;
    }

    public void doCurrencyConversionRateToVendor(WalletBalanceVo walletBalanceVo, BigDecimal conversionRate) {

        //walletBalanceVo consists of balance need to do conversion when they are not 0 or null.

        BigDecimal balance = walletBalanceVo.getData().getBalance();

        if (balance != BigDecimal.ZERO && balance != null) {
            walletBalanceVo.getData().setBalance(stripZeroToString(balance.multiply(conversionRate)));
        }

    }

    private BigDecimal stripZeroToString(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
