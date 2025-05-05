package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ObjectBuffer;

/**
 * Deserializer implementation that is used if it is necessary to bind content of
 * "unknown" type; something declared as basic {@link java.lang.Object}
 * (either explicitly, or due to type erasure).
 * If so, "natural" mapping is used to convert JSON values to their natural
 * Java object matches: JSON arrays to Java {@link java.util.List}s (or, if configured,
 * Object[]), JSON objects to {@link java.util.Map}s, numbers to
 * {@link java.lang.Number}s, booleans to {@link java.lang.Boolean}s and
 * strings to {@link java.lang.String} (and nulls to nulls).
 */
@JacksonStdImpl
public class UntypedObjectDeserializer
    extends StdDeserializer<Object>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;
    private static final int MAX_DEPTH = 1000;
    
    private final static Object[] NO_OBJECTS = new Object[0];

    /**
     * @since 2.2
     */
    public final static UntypedObjectDeserializer instance = new UntypedObjectDeserializer();
    
    /*
    /**********************************************************
    /* Possible custom deserializer overrides we need to use
    /**********************************************************
     */

    protected JsonDeserializer<Object> _mapDeserializer;

    protected JsonDeserializer<Object> _listDeserializer;

    protected JsonDeserializer<Object> _stringDeserializer;

    protected JsonDeserializer<Object> _numberDeserializer;
    
    public UntypedObjectDeserializer() { super(Object.class); }

    @SuppressWarnings("unchecked")
    public UntypedObjectDeserializer(UntypedObjectDeserializer base,
            JsonDeserializer<?> mapDeser, JsonDeserializer<?> listDeser,
            JsonDeserializer<?> stringDeser, JsonDeserializer<?> numberDeser)
    {
        super(Object.class);
        _mapDeserializer = (JsonDeserializer<Object>) mapDeser;
        _listDeserializer = (JsonDeserializer<Object>) listDeser;
        _stringDeserializer = (JsonDeserializer<Object>) stringDeser;
        _numberDeserializer = (JsonDeserializer<Object>) numberDeser;
    }
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _findCustomDeser(DeserializationContext ctxt, JavaType type)
        throws JsonMappingException
    {
        // NOTE: since we don't yet have the referring property, this should be fine:
        JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(type);
        if (ClassUtil.isJacksonStdImpl(deser)) {
            return null;
        }
        return (JsonDeserializer<Object>) deser;
    }
    
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        JsonDeserializer<?> mapDeserializer = _mapDeserializer;
        if (mapDeserializer instanceof ContextualDeserializer) {
            mapDeserializer = ((ContextualDeserializer)mapDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> listDeserializer = _listDeserializer;
        if (listDeserializer instanceof ContextualDeserializer) {
            listDeserializer = ((ContextualDeserializer)listDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> stringDeserializer = _stringDeserializer;
        if (stringDeserializer instanceof ContextualDeserializer) {
            stringDeserializer = ((ContextualDeserializer)stringDeserializer).createContextual(ctxt, property);
        }
        JsonDeserializer<?> numberDeserializer = _numberDeserializer;
        if (numberDeserializer instanceof ContextualDeserializer) {
            numberDeserializer = ((ContextualDeserializer)numberDeserializer).createContextual(ctxt, property);
        }

        // And if anything changed, we'll need to change too!
        if ((mapDeserializer != _mapDeserializer)
                || (listDeserializer != _listDeserializer)
                || (stringDeserializer != _stringDeserializer)
                || (numberDeserializer != _numberDeserializer)
                ) {
            return _withResolved(mapDeserializer, listDeserializer,
                    stringDeserializer, numberDeserializer);
        }
        return this;
    }

    protected JsonDeserializer<?> _withResolved(JsonDeserializer<?> mapDeser,
            JsonDeserializer<?> listDeser,
            JsonDeserializer<?> stringDeser, JsonDeserializer<?> numberDeser) {
        return new UntypedObjectDeserializer(this,
                mapDeser, listDeser, stringDeser, numberDeser);
    }
    /*
    /**********************************************************
    /* Deserializer API
    /**********************************************************
     */
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return _deserialize(jp, ctxt, 0);
    }
    private Object _deserialize(JsonParser jp, DeserializationContext ctxt, int depth) throws IOException {
        switch (jp.getCurrentToken()) {
            case FIELD_NAME:
            case START_OBJECT:
                if (_mapDeserializer != null) {
                    return _mapDeserializer.deserialize(jp, ctxt);
                }
                if (depth > MAX_DEPTH) {
                    throw new JsonParseException("JSON is too deeply nested.", jp.getCurrentLocation());
                }
                return mapObject(jp, ctxt, depth);
            case START_ARRAY:
                if (depth > MAX_DEPTH) {
                    throw new JsonParseException("JSON is too deeply nested.", jp.getCurrentLocation());
                }
                if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
                    return mapArrayToArray(jp, ctxt, depth);
                }
                if (_listDeserializer != null) {
                    return _listDeserializer.deserialize(jp, ctxt);
                }
                return mapArray(jp, ctxt, depth);
            case VALUE_EMBEDDED_OBJECT:
                return jp.getEmbeddedObject();
            case VALUE_STRING:
                if (_stringDeserializer != null) {
                    return _stringDeserializer.deserialize(jp, ctxt);
                }
                return jp.getText();
            case VALUE_NUMBER_INT:
                if (_numberDeserializer != null) {
                    return _numberDeserializer.deserialize(jp, ctxt);
                }
                /* [JACKSON-100]: caller may want to get all integral values
                 * returned as BigInteger, for consistency
                 */
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return jp.getBigIntegerValue(); // should be optimal, whatever it is
                }
                return jp.getNumberValue(); // should be optimal, whatever it is
            case VALUE_NUMBER_FLOAT:
                if (_numberDeserializer != null) {
                    return _numberDeserializer.deserialize(jp, ctxt);
                }
                /* [JACKSON-72]: need to allow overriding the behavior regarding
                 *   which type to use
                 */
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    return jp.getDecimalValue();
                }
                return Double.valueOf(jp.getDoubleValue());
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            case VALUE_NULL: // should not get this but...
                return null;
            case END_ARRAY: // invalid
            case END_OBJECT: // invalid
            default:
                throw ctxt.mappingException(Object.class);
        }
    }
    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException {
        JsonToken t = jp.getCurrentToken();
        switch (t) {
        // First: does it look like we had type id wrapping of some kind?
        case START_ARRAY:
        case START_OBJECT:
        case FIELD_NAME:
            /* Output can be as JSON Object, Array or scalar: no way to know
             * a this point:
             */
            return typeDeserializer.deserializeTypedFromAny(jp, ctxt);

        /* Otherwise we probably got a "native" type (ones that map
         * naturally and thus do not need or use type ids)
         */
        case VALUE_STRING:
            return jp.getText();

        case VALUE_NUMBER_INT:
            // For [JACKSON-100], see above:
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                return jp.getBigIntegerValue();
            }
            /* and as per [JACKSON-839], allow "upgrade" to bigger types: out-of-range
             * entries can not be produced without type, so this should "just work",
             * even if it is bit unclean
             */
            return jp.getNumberValue();

        case VALUE_NUMBER_FLOAT:
            // For [JACKSON-72], see above
            if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return jp.getDecimalValue();
            }
            return Double.valueOf(jp.getDoubleValue());

        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_FALSE:
            return Boolean.FALSE;
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();

        case VALUE_NULL: // should not get this far really but...
            return null;
        default:
            throw ctxt.mappingException(Object.class);
        }
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Method called to map a JSON Array into a Java value.
     */
    protected Object mapArray(JsonParser jp, DeserializationContext ctxt, int depth)
        throws IOException, JsonProcessingException
    {
        if (ctxt.isEnabled(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)) {
            return mapArrayToArray(jp, ctxt, depth);
        }
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            return new ArrayList<Object>(4);
        }
        ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] values = buffer.resetAndStart();
        int ptr = 0;
        int totalSize = 0;
        do {
            Object value = _deserialize(jp, ctxt, depth + 1);
            ++totalSize;
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (jp.nextToken() != JsonToken.END_ARRAY);
        // let's create almost full array, with 1/8 slack
        ArrayList<Object> result = new ArrayList<Object>(totalSize + (totalSize >> 3) + 1);
        buffer.completeAndClearBuffer(values, ptr, result);
        return result;
    }

    /**
     * Method called to map a JSON Object into a Java value.
     */
    protected Object mapObject(JsonParser jp, DeserializationContext ctxt, int depth)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        // 1.6: minor optimization; let's handle 1 and 2 entry cases separately
        if (t != JsonToken.FIELD_NAME) { // and empty one too
            // empty map might work; but caller may want to modify... so better just give small modifiable
            return new LinkedHashMap<String,Object>(4);
        }
        String field1 = jp.getText();
        jp.nextToken();
        Object value1 = _deserialize(jp, ctxt, depth + 1);
        if (jp.nextToken() != JsonToken.FIELD_NAME) { // single entry; but we want modifiable
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
            result.put(field1, value1);
            return result;
        }
        String field2 = jp.getText();
        jp.nextToken();
        Object value2 = _deserialize(jp, ctxt, depth + 1);
        if (jp.nextToken() != JsonToken.FIELD_NAME) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(4);
            result.put(field1, value1);
            result.put(field2, value2);
            return result;
        }
        // And then the general case; default map size is 16
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(field1, value1);
        result.put(field2, value2);
        do {
            String fieldName = jp.getText();
            jp.nextToken();
            result.put(fieldName, _deserialize(jp, ctxt, depth+1));
        } while (jp.nextToken() != JsonToken.END_OBJECT);
        return result;
    }

    /**
     * Method called to map a JSON Array into a Java Object array (Object[]).
     */
    protected Object[] mapArrayToArray(JsonParser jp, DeserializationContext ctxt, int depth)
        throws IOException, JsonProcessingException
    {
        // Minor optimization to handle small lists (default size for ArrayList is 10)
        if (jp.nextToken()  == JsonToken.END_ARRAY) {
            return NO_OBJECTS;
        }
        ObjectBuffer buffer = ctxt.leaseObjectBuffer();
        Object[] values = buffer.resetAndStart();
        int ptr = 0;
        do {
            Object value = _deserialize(jp, ctxt, depth + 1);
            if (ptr >= values.length) {
                values = buffer.appendCompletedChunk(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (jp.nextToken() != JsonToken.END_ARRAY);
        return buffer.completeAndClearBuffer(values, ptr);
    }
}
