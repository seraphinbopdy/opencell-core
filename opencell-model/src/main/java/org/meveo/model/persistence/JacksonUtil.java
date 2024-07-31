/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */

package org.meveo.model.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.meveo.model.customEntities.CustomEntityInstance;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonUtil {

    public static final TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<Map<String, String>>() {
    };
    public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>() {
    };

    private static final ThreadLocal<ObjectMapper> OBJECT_MAPPER = new ThreadLocal<ObjectMapper>() {
        @Override
        protected ObjectMapper initialValue() {
            ObjectMapper om = new ObjectMapper();
            om.setVisibility(om.getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
            om.setVisibility(om.getVisibilityChecker().withGetterVisibility(JsonAutoDetect.Visibility.NONE));
            om.setVisibility(om.getVisibilityChecker().withIsGetterVisibility(Visibility.NONE));
            om.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            om.setSerializationInclusion(Include.NON_NULL);

            return om;
        }
    };

    public static <T> T fromString(String string, Class<T> clazz) {
        try {
            ObjectMapper om = OBJECT_MAPPER.get();
            return om.readValue(string, clazz);

        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: " + string + " cannot be transformed to Json object", e);
        }
    }

    public static <T> T fromString(String string, TypeReference<T> typeReference) {
        try {
            ObjectMapper om = OBJECT_MAPPER.get();
            return om.readValue(string, typeReference);

        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: " + string + " cannot be transformed to Json object", e);
        }
    }

    public static String toString(Object value) {
        try {
            ObjectMapper om = OBJECT_MAPPER.get();
            return om.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: " + value + " cannot be transformed to a String", e);
        }
    }

    public static String toStringPrettyPrinted(Object value) {
        try {
            ObjectMapper om = OBJECT_MAPPER.get();
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: " + value + " cannot be transformed to a String", e);
        }
    }

    public static String beautifyString(String jsonString) {
        Object obj = fromString(jsonString, Object.class);
        return toStringPrettyPrinted(obj);
    }

    public static JsonNode toJsonNode(String value) {
        try {
            ObjectMapper om = OBJECT_MAPPER.get();
            return om.readTree(value);

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JsonNode toJsonNode(Object value) {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.valueToTree(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T value) {
        return fromString(toString(value), (Class<T>) value.getClass());
    }

    public static Object convert(Object value, JavaType jacksonType) {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.convertValue(value, jacksonType);
    }

    public static <T> T convert(Object value, Class<T> clazz) {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.convertValue(value, clazz);
    }

    public static <T> T convert(Object value, TypeReference<T> typeref) {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.convertValue(value, typeref);
    }

    public static Map<String, Object> convertToMap(CustomEntityInstance value) {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.convertValue(value, MAP_STRING_OBJECT);
    }

    public static <T> T read(String value, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, clazz);
    }

    public static <T> T read(File value, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, clazz);
    }

    public static <T> T read(File value, TypeReference<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, clazz);
    }

    public static <T> T read(InputStream value, Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, clazz);
    }

    public static <T> T read(InputStream value, TypeReference<T> clazz) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, clazz);
    }

    public static <T> T read(String string, TypeReference<T> typeReference) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(string, typeReference);
    }

    public static Map<String, Object> toMap(InputStream value) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, MAP_STRING_OBJECT);
    }

    public static Map<String, Object> toMap(Object value) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.convertValue(value, MAP_STRING_OBJECT);
    }

    public static List<Map<String, Object>> toList(InputStream value) throws JsonParseException, JsonMappingException, IOException {
        var typeRef = new TypeReference<List<Map<String, Object>>>() {
        };
        ObjectMapper om = OBJECT_MAPPER.get();
        return om.readValue(value, typeRef);
    }
}