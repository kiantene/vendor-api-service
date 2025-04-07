package com.nextgen.gameaggregator.vendor.aasexy.api.routing;

import com.nextgen.gameaggregator.vendor.aasexy.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH, consumes = {MediaType.APPLICATION_JSON_VALUE})
public class RoutingAction {

    private final ApplicationContext applicationContext;
    private final Map<String, Class<?>> v1Controllers = Map.of(
            "action", com.nextgen.gameaggregator.vendor.aasexy.api.action.GeneralAction.class
    );
    private final Map<String, Class<?>> v2Controllers = Map.of(
            "action", com.nextgen.gameaggregator.vendor.aasexyv2.api.action.GeneralAction.class
    );
    @Value("${aasexy-version:v1}") // Default to v1 if not set
    private String toggleVersion;

    public RoutingAction(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostMapping("/{action}")
    public ResponseEntity<?> routeRequest(@PathVariable String action, HttpServletRequest request) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object controllerBean;
        Object result;
        Method method;

        boolean useV2 = "v2".equalsIgnoreCase(toggleVersion);
        Class<?> controllerClass = useV2 ? v2Controllers.get(action) : v1Controllers.get(action);

        try {
            controllerBean = applicationContext.getBean(controllerClass);
            method = controllerBean.getClass().getMethod(action + "Request", HttpServletRequest.class);

            result = method.invoke(controllerBean, request);
            return convertToResponseEntity(result);

        } catch (Exception e) {
            controllerClass = v1Controllers.get(action);
            controllerBean = applicationContext.getBean(controllerClass);
            method = controllerBean.getClass().getMethod(action + "Request", HttpServletRequest.class);

            result = method.invoke(controllerBean, request);
            return convertToResponseEntity(result);

        }
    }

    private ResponseEntity<?> convertToResponseEntity(Object result) {
        if (result instanceof ResponseEntity<?>) {
            return (ResponseEntity<?>) result; // ✅ Already a ResponseEntity, return as-is
        }
        return ResponseEntity.ok(result); // ✅ Wrap other responses in ResponseEntity
    }
}
