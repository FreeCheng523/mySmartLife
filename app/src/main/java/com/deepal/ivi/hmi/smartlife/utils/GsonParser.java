package com.deepal.ivi.hmi.smartlife.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class GsonParser {

    /**
     * 针对无泛型、非数组集合的情况
     * @param json
     * @return
     * @param <T>
     */
    public static <T> T parse(String json) {
        if(!TextUtils.isEmpty(json)){
            Type type = new TypeToken<T>() {}.getType();
            T data = new Gson().fromJson(json, type);
            return data;
        }else{
            return null;
        }
    }

    public static <T> T parse(String json,Type type) {
        if(!TextUtils.isEmpty(json)){
            T data = new Gson().fromJson(json, type);
            return data;
        }else{
            return null;
        }
    }

    public static <T> List<T> parseList(String json, Class<T> cls) {
        if(!TextUtils.isEmpty(json)){
            Type type = new ParameterizedTypeImpl(cls);
            List<T> list =  new Gson().fromJson(json, type);
            return list;
        }else{
            return null;
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {
        Class clazz;

        public ParameterizedTypeImpl(Class clz) {
            clazz = clz;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{clazz};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
