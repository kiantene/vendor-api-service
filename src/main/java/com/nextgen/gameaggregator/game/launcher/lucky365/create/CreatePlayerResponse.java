package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePlayerResponse {

    private String code;

    public boolean isSuccess() {
        return code != null && (code.equals(ResponseCodes.SUCCESS.getCode())||code.equals(ResponseCodes.PLAYER_EXISTING.getCode()));
    }


}