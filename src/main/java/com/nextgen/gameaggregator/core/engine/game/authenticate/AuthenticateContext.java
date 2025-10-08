package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class AuthenticateContext extends VendorRequestContext implements GameSessionData {
}
