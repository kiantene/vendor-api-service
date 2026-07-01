package com.nextgen.gameaggregator.game.launcher.mtlive.member.create;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberCreateDataResponse {
    private Integer result;
}
