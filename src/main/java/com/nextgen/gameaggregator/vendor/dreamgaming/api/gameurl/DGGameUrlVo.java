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

    private String tableId; // use for map game url

    //GA-13216: redirect to site lobby URL
    private String backUrl;
    private String backType;

    @Override
    public String getGameUrl() {
        if (list == null || list.isEmpty()) {
            return null;
        }

        StringBuilder url = new StringBuilder(list.get(0));

        if (tableId != null && !tableId.isBlank()) {
            url.append("&tableId=").append(tableId);
        }

        if (backUrl != null && !backUrl.isBlank()) {
            url.append("&backUrl=").append(backUrl);
        }

        return url.toString();
    }
}
