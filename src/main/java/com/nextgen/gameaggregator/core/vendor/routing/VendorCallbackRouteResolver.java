package com.nextgen.gameaggregator.core.vendor.routing;

import java.util.Optional;

public interface VendorCallbackRouteResolver {
    Optional<String> resolveTargetUri(VendorRouteContext context);
}
