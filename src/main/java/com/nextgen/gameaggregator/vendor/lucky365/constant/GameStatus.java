package com.nextgen.gameaggregator.vendor.lucky365.constant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameStatus {

    NORMAL(0, "Normal", false),
    FREE(1, "Free", false),
    BONUS1(2, "Bonus1", false),
    BONUS2(3, "Bonus2", false),
    BONUS3(4, "Bonus3", false),
    BONUS4(5, "Bonus4", false),

    GAMBLE(224, "Gamble", false),

    JACKPOT1(240, "Jackpot1", true),
    JACKPOT0(241, "Jackpot0", true),
    JACKPOT2(244, "Jackpot2", true),
    JACKPOT_MILLION(247, "JackpotMillion", true),
    JACKPOT0_MILLION(248, "Jackpot0Million", true),
    JACKPOT1_MILLION(249, "Jackpot1Million", true),
    JACKPOT2_MILLION(250, "Jackpot2Million", true),

    RED_PACKET(242, "红包", false),
    TREASURE_BOX(243, "宝箱", false),
    LUCKY_WHEEL1(245, "幸运大轮盘1", false),
    LUCKY_WHEEL2(246, "幸运大轮盘2", false);

    private final int code;
    private final String message;
    private final boolean isJackpot;


    public static GameStatus fromCode(int code) {
        for (GameStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown GameStatus code: " + code);
    }
}
