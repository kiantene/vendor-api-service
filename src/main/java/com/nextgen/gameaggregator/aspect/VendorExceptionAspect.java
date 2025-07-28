package com.nextgen.gameaggregator.aspect;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapperRegistry;
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
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            String vendorClassName = vendorHandler.className();
            VendorExceptionMapper mapper = registry.getMapper(vendorClassName);

            if (mapper != null) {
                return mapper.handle(ex);
            }
            throw ex;
        }
    }
}
