package com.nextgen.gameaggregator.vendor.dreamgaming.api.gameurl;

import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DGGameUrlVo implements GameUrlVo {

    @NotBlank(message = "url can not be blank")
    private List<String> list;  // Change from String to List<String>

    @Override
    public String getGameUrl() {
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }
}
