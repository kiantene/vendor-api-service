package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GameSessionRefreshContext extends VendorRequestContext {
    
}
