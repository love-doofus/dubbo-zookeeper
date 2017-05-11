package com.wang.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

/**
 * 序列化工具
 * @author wxe
 * @since 1.0.0
 */
public abstract class JsonUtils {

    public static String toJson(Object o) {
        return JSON.toJSONString(o);
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
    
    public static <T> T fromJson(String text, TypeReference<T> type) {
        return JSON.parseObject(text, type);
    }

}
