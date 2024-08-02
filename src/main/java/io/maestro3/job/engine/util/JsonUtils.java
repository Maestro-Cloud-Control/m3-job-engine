package io.maestro3.job.engine.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.maestro3.job.engine.exception.JsonConversionException;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JodaModule())
            .registerModule(new JavaTimeModule());

    private JsonUtils() {
        throw new UnsupportedOperationException("Class is not designed for an instantiation");
    }

    public static <T> T parseJson(final String json, final TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new JsonConversionException("Failed to parse json", e);
        }
    }

    public static String convertToJson(final Object object) {
        try {
            return MAPPER.writer().writeValueAsString(object);
        } catch (Exception e) {
            throw new JsonConversionException("Failed to convert object to json", e);
        }
    }
}
