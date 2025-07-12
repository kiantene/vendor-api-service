package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.common.WebClientApiCaller;
import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
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

    public GameLaunchService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public void processLaunchRequest(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {

        switch (launchHandler.getLaunchMode()) {
            case API_CALL -> callExternalApi(context, launchHandler);
//            case HTML_RESPONSE -> handleHtmlResponse(context, handler);
            case STATIC_HTML -> buildStaticHtml(context, launchHandler);
//            case ENCRYPTED_API_CALL -> callEncryptedApi(context, handler);
            case QUERY_STRING_URL -> buildQueryStringUri(context, launchHandler);
            default -> throw new UnsupportedOperationException("Unsupported launch mode");
        }
    }

    private void callExternalApi(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {
        String vendorClassName = context.getVendorClassName();
        Object request = launchHandler.onPrepareRequestBody(context);

        String baseUrl = launchHandler.getBaseUrl(context);
        if (baseUrl == null) throw new InternalConfigurationException(vendorClassName + " Game Launch baseUrl cannot be found.");

        Map<String, String> headers = launchHandler.getHeaders(context, request);

        WebClientApiCaller webClientApiCaller = new WebClientApiCaller(
                launchHandler.getPath(),
                launchHandler.getContentType()
        );

        // should fire error event?
        Object response = webClientApiCaller.post(
                baseUrl,
                headers,
                request,
                launchHandler.getResponseType()
        );

        launchHandler.onSuccess(context, response);
    }

    private void buildStaticHtml(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {
        @SuppressWarnings("unchecked")
        Map<String, String> request = convertToMap(launchHandler.onPrepareRequestBody(context));
        String htmlTemplate = launchHandler.getHtmlTemplate();
        String html = applyPlaceholderReplacement(htmlTemplate, request);
        String response = s3Service.generateHtmlToS3(context, html);
        context.setGameUrl(response);
        launchHandler.onSuccess(context, response);
    }

    private void buildQueryStringUri(GameLaunchContext context, GameLaunchHandler<Object, Object> launchHandler) {
        Object request = launchHandler.onPrepareRequestBody(context);
        MultiValueMap<String, String> formData = convertToMultiValueMap(request);
        String baseUrl = launchHandler.getBaseUrl(context);
        String path = launchHandler.getPath();

        String gameUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

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
