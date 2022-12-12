package com.nextgen.gameaggregator.vendor.api.pgsoft.component.action;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

public class AbstractAction {

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
}
