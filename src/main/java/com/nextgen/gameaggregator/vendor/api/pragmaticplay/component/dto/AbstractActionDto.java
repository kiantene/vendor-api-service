package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AbstractActionDto {

    public <T> T queryStringToDto(String queryString, Class<T> clazz) {

        System.out.println(queryString);

        HashMap<String, Object> queryParameterMap = new HashMap<String, Object>();
        String[] fields = queryString.split("&");

        for (int i = 0; i < fields.length; ++i) {
            String[] kv = fields[i].split("=");
            if (2 == kv.length) {
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        T t = mapper.convertValue(queryParameterMap, clazz);

        return t;
    }

    public <T> Map<String, String> doValidation(T dto, Class<T> clazz) {
        Map<String, String> validationMap = new HashMap<String, String>();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        for (ConstraintViolation<T> violation : violations) {
            validationMap.put(violation.getPropertyPath().toString(), violation.getPropertyPath() + " " + violation.getMessage());
        }
        return validationMap;
    }

    //region handle query string data to map object
    public HashMap<String, Object> handleQueryStringDataToMapObject(String queryString){

        HashMap<String, Object> things = new HashMap<String, Object>();
        String[] fields = queryString.split("&");
        String[] kv = new String[0];

        for (int i = 0; i < fields.length; ++i)
        {
            kv = fields[i].split("=");
            if (2 == kv.length)
            {
                things.put(kv[0], kv[1]);
            }
        }

        return things;
    }
    //endregion
}
