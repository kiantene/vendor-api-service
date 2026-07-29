package com.nextgen.gameaggregator.core.engine.operator.wallet;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorScenario;

/**
 * Mapper interface for transforming vendor-specific context into an operator-specific request object.
 *
 * <p>
 * This interface defines a generic contract for converting:
 * - OperatorApiContext: contains the vendor context, game round, and transaction
 * - OperatorScenario: the specific scenario (bet, result, combined, etc.)
 * <p>
 * Into:
 * - R: a vendor-specific operator request object, ready to be sent via OperatorApiAdapter.
 * </p>
 *
 * <p>
 * Key points:
 * - Generic parameters allow flexibility:
 *   - C: VendorRequestContext subtype for vendor-specific data
 *   - R: Operator request DTO type
 *   - S: OperatorScenario subtype
 * - Keeps mapping logic separate from execution logic, preserving single responsibility.
 * - Enables clean separation between domain models and operator-specific request models.
 * </p>
 *
 * <p>
 * Usage:
 * - Implemented per type of operator wallet request.
 * - Called by OperatorApiService or adapters to transform internal context into a request object
 *   before sending it to the operator API.
 * </p>
 */
public interface OperatorRequestMapper<
        C extends VendorRequestContext,
        R,
        S extends OperatorScenario> {

    R toOperatorRequest(OperatorApiContext<C> context, S scenario);

}