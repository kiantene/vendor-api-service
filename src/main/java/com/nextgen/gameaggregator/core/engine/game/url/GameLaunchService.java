package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.core.webclient.VendorApiExecutor;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.service.S3Service;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Service
public class GameLaunchService {
    private final S3Service s3Service;
    private final VendorApiExecutor apiExecutor;

    public GameLaunchService(S3Service s3Service,
                             VendorApiExecutor apiExecutor) {

        this.s3Service = s3Service;
        this.apiExecutor = apiExecutor;
    }

    public void processLaunchRequest(GameLaunchContext context, AbstractGameLaunchHandler<Object, Object> launchHandler) {
        LogContext logContext = populateLogContext(context);
        try {
            switch (launchHandler.getLaunchMode()) {
                case API_CALL -> callExternalApi(context, launchHandler);
//                case HTML_RESPONSE -> handleHtmlResponse(context, handler);
                case STATIC_HTML -> buildStaticHtml(context, launchHandler);
//                case ENCRYPTED_API_CALL -> callEncryptedApi(context, handler);
                case QUERY_STRING_URL -> buildQueryStringUri(context, launchHandler);
                default -> throw new UnsupportedOperationException("Unsupported launch mode");
            }
        } finally {
            long endTime = System.currentTimeMillis();
            logContext.setApiEnd(endTime);
            logContext.setApiTimeTaken(endTime - logContext.getApiStart());
        }
    }

    private LogContext populateLogContext(GameLaunchContext context) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) return new LogContext();

        logContext.setVendorId(context.getVendorId());
        logContext.setAgentId(context.getAgentId());
        logContext.setUsername(context.getAgentPlayerUsername());
        logContext.setApiStart(System.currentTimeMillis());
        return logContext;
    }

    private void callExternalApi(GameLaunchContext context, AbstractGameLaunchHandler<Object, Object> launchHandler) {
        launchHandler.execute(apiExecutor, context)
                .onSuccess(response -> {
                    launchHandler.onSuccess(context, response);
                });

//        String vendorClassName = context.getVendorClassName();
//        String baseUrl = launchHandler.getBaseUrl(context);
//        if (baseUrl == null) throw new InternalConfigurationException(vendorClassName + " Game Launch baseUrl cannot be found.");
//        Object request = launchHandler.buildRequestBody(context);
//        Map<String, String> headers = launchHandler.getHeaders(context, request);
//
//        WebClientApiCaller webClientApiCaller = new WebClientApiCaller(
//                launchHandler.getPath(context),
//                launchHandler.getContentType()
//        );
//
//        // should fire error event?
//        Object response = webClientApiCaller.post(
//                baseUrl,
//                headers,
//                request,
//                launchHandler.getResponseType()
//        );
//
//        launchHandler.onSuccess(context, response);
    }

    private void buildStaticHtml(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {
        Map<String, String> request = convertToMap(launchHandler.buildRequestBody(context));
        String htmlTemplate = launchHandler.getHtmlTemplate();
        String html = applyPlaceholderReplacement(htmlTemplate, request);
        String response = s3Service.generateHtmlToS3(context, html);
        context.setGameUrl(response);
        launchHandler.onSuccess(context, response);
    }

    private void buildQueryStringUri(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {
        String baseUrl = launchHandler.getBaseUrl(context);
        String path = launchHandler.getPath(context);
        Object request = launchHandler.buildRequestBody(context);
        MultiValueMap<String, String> formData = convertToMultiValueMap(request);

        String gameUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        LogContext logContext = LogContextHolder.get();
        try {
            logContext.setApiBody(request);
            logContext.setApiResponse(gameUrl);
        } catch (Exception exception) {
            logContext.setException(exception.getClass().getName());
            logContext.setErrorMessage(exception.getMessage());
        }

        launchHandler.onSuccess(context, gameUrl);
    }

    private String applyPlaceholderReplacement(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private static Map<String, String> convertToMap(Object dto) {
        Map<String, String> map = new HashMap<>();
        if (dto == null) {
            return map;
        }

        Field[] fields = dto.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(dto);
                if (value != null) {
                    map.put(field.getName(), value.toString());
                }
            } catch (IllegalAccessException e) {
                // Optionally log or rethrow if needed
            }
        }
        return map;
    }

    public MultiValueMap<String, String> convertToMultiValueMap(Object dto) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        BeanWrapper wrapper = new BeanWrapperImpl(dto);

        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            Object value = wrapper.getPropertyValue(name);
            if (value != null && !"class".equals(name)) {
                map.add(name, value.toString());
            }
        }

        return map;
    }
}
