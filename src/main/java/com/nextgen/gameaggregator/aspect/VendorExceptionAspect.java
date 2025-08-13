package com.nextgen.gameaggregator.aspect;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapperRegistry;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class VendorExceptionAspect {
    private final VendorExceptionMapperRegistry registry;

    public VendorExceptionAspect(VendorExceptionMapperRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(vendorHandler)")
    public Object handleVendorExceptions(ProceedingJoinPoint joinPoint,
                                                    VendorExceptionHandler vendorHandler) throws Throwable {

        String vendorClassName = vendorHandler.className();
        VendorExceptionMapper mapper = registry.getMapper(vendorClassName);

        if (mapper == null) {
            throw new InternalConfigurationException("No exception mapper registered for vendor: " + vendorClassName);
        }

        LogContext logContext = LogContextHolder.get();
        VendorErrorResponse errorResponse;

        try {
            return joinPoint.proceed();

        } catch (GameSessionExpiredException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onGameSessionExpired(ex);

        } catch (GameTerminatedException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onGameTerminated(ex);

        } catch (InsufficientBalanceException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onInsufficientBalance(ex);

        } catch (PlayerDisabledException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onPlayerDisabled(ex);

        } catch (BetNotAllowedException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onBetNotAllowed(ex);

        } catch (DuplicateBetException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onDuplicateBet(ex);

        } catch (DuplicateRequestException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onDuplicateRequest(ex);

        } catch (EntityNotFoundException ex) {
            InternalConfigurationException configurationException = new InternalConfigurationException(ex.getMessage(), ex);
            logContext.setException(ex);
            errorResponse = mapper.onInternalConfigurationError(configurationException);

        } catch (InternalConfigurationException ex) {
            logContext.setException(ex);
            errorResponse = mapper.onInternalConfigurationError(ex);

        } catch (InvalidOperatorResponseException ex) {
            logContext.setException(ex);
            logContext.setRootCause(ex.getRootCause());
            errorResponse = mapper.onInternalError(new InternalServerException(ex.getMessage(), ex));

        } catch (Exception ex) {
            logContext.setException(ex);
            errorResponse = mapper.onInternalError(new InternalServerException(ex.getMessage(), ex));
        }
        return new ResponseEntity<>(errorResponse.getBody(), errorResponse.getStatusCode());
    }
}
