package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.vendor.evoplay.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.endround.WinDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DataDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DetailsDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvoplayBonusSpinDtoTest {

    @Test
    void betDto_marksBonusSpinsAsFreespinEvenWhenFreespinFlagIsFalse() {
        BetDto dto = new BetDto();
        dto.setData(bonusSpinData());

        assertThat(dto.getIsFreespin()).isEqualTo(1);
    }

    @Test
    void winDto_marksBonusSpinsAsFreespinEvenWhenFreespinFlagIsFalse() {
        WinDto dto = new WinDto();
        dto.setData(bonusSpinData());

        assertThat(dto.getIsFreespin()).isEqualTo(1);
    }

    private DataDto bonusSpinData() {
        DetailsDto details = new DetailsDto();
        details.setFreespin("false");
        details.setRound_mode("bonus_spins");
        details.setExtrabonus_type("bonus_spins");

        DataDto data = new DataDto();
        data.setDetailsDto(details);
        return data;
    }
}
