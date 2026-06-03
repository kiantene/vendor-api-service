package com.nextgen.gameaggregator.vendor.spribe.constant;

import java.util.List;
import java.util.Set;

public class FreeBetAction {
    // Used by BetResultController.configure() to enable setBetAndResult for fallback-path actions.
    public static final List<String> list = List.of("rainfreebet", "promofreebet", "challengefreebet", "challengeprizemoney");

    // All action types that arrive on /deposit without a preceding /withdraw.
    // Used by SpribeRouteResolver to bypass the game-transaction lookup for freebet deposits.
    public static final Set<String> NO_BET_ACTIONS = Set.of(
            "freebet", "rainfreebet", "promofreebet", "challengefreebet", "challengeprizemoney"
    );
}