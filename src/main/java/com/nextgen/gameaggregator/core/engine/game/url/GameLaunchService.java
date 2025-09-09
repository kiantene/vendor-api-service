package com.nextgen.gameaggregator.core.engine.game.url;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.webclient.VendorApiExecutor;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GameLaunchService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final S3Service s3Service;
    private final VendorApiExecutor apiExecutor;

    public GameLaunchService(S3Service s3Service,
                             VendorApiExecutor apiExecutor) {

        this.s3Service = s3Service;
        this.apiExecutor = apiExecutor;
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
        } catch (GameLaunchException ex) {
            logContext.setException(ex);
            throw ex;
        } catch (Exception ex) {
            GameLaunchException gameLaunchException = new GameLaunchException(ex.getMessage(), ex);
            logContext.setException(gameLaunchException);
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            logContext.setApiEnd(endTime);
            logContext.setApiTimeTaken(endTime - logContext.getApiStart());
        }
    }

    private LogContext populateLogContext(GameLaunchContext context) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            logContext = new LogContext();
        }

        logContext.setVendorId(context.getVendorId());
        logContext.setAgentId(context.getAgentId());
        logContext.setUsername(context.getAgentPlayerUsername());
        logContext.setApiStart(System.currentTimeMillis());
        return logContext;
    }

    private void callExternalApi(GameLaunchContext context, AbstractGameLaunchHandler<Object, Object> launchHandler) {
        LogContext logContext = LogContextHolder.get();
        launchHandler
                .prepareLaunchRequest(context)
                .execute(apiExecutor, context)
                .onSuccess(response -> launchHandler.onSuccess(context, response))
                .onError(result -> logContext.setException(result.getError()))
                .onComplete(result -> {
                    try {
                        String requestBody = OBJECT_MAPPER.writeValueAsString(result.getRequestObject());
                        context.setVendorFormData(requestBody);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                    }

                    logContext.setApiResponse(result.getRawResponse());
                });
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

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl + path);
        applyEncodedQueryParams(urlBuilder, formData);

        String gameUrl = urlBuilder.build(true)
                .toUri()
                .toString();

        LogContext logContext = LogContextHolder.get();
        try {
            logContext.setApiBody(request);
            logContext.setApiResponse(gameUrl);
            context.setVendorFormData(OBJECT_MAPPER.writeValueAsString(request));
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

    private static void applyEncodedQueryParams(
            UriComponentsBuilder builder,
            MultiValueMap<String, String> rawParams
    ) {
        for (Map.Entry<String, List<String>> entry : rawParams.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                builder.queryParam(key, encodedValue);
            }
        }
    }
}
