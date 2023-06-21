package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailsDto {
    private List<?> symbols;
    private GameDto game;
    private DetailsDetailsDto details;
    private String denomination;
    private CurrencyRateDto currency_rate;
    private String bet;
    private String lines;
    private String total_bet;
    private String total_win;
    private String final_action;
    private String round_mode;
    private String balance_after_pay;
    private String payout;
    private String lent_pack_id;
    private String freespin;
    private String single_spin;
    private String freespins_left;
    private String balance_before_pay;
    private String pay_for_action_this_round;
    private String game_mode_code;
    private String total_bet_for_action_in_money;
    private String total_win_for_action_in_money;
    private RoundDto round;

}
