package com.nextgen.gameaggregator.aspect;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapperRegistry;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
        VendorExceptionMapper<Object> mapper = registry.getMapper(vendorClassName);

        if (mapper == null) {
            throw new InternalConfigurationException("No exception mapper registered for vendor: " + vendorClassName);
        }

        try {
            return joinPoint.proceed();
        } catch (EntityNotFoundException ex) {
            InternalConfigurationException configurationException = new InternalConfigurationException(ex);
            return mapper.onInternalConfigurationError(configurationException);
        } catch (InternalConfigurationException ex) {
            return mapper.onInternalConfigurationError(ex);
        } catch (OperatorNetworkException ex) {
            return mapper.onInternalConfigurationError(new InternalConfigurationException(ex));
        } catch (AuthenticationException ex) {
            return mapper.onAuthenticationError(ex);
        }
    }
}
