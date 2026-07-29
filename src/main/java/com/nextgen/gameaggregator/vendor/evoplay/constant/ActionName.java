package com.nextgen.gameaggregator.vendor.evoplay.constant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ActionName {
    init, bet, win, refund, balanceincrease;

    private static final Set<ActionName> ROUTING_ACTIONS =
            Collections.unmodifiableSet(EnumSet.of(bet, win, refund));

    private static final Set<String> ROUTING_ACTION_NAMES =
            ROUTING_ACTIONS.stream()
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());

    public static Set<ActionName> getActionsRelevantForRouting() {
        return ROUTING_ACTIONS;
    }

    public static Set<String> getActionNamesRelevantForRouting() {
        return ROUTING_ACTION_NAMES;
    }


}
