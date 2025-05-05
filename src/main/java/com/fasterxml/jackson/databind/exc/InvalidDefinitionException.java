package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Intermediate exception type used as the base class for all {@link JsonMappingException}s
 * that are due to problems with target type definition; usually a problem with
 * annotations used on a class or its properties.
 * This is in contrast to {@link MismatchedInputException} which
 * signals a problem with input to map.
 *
 * @since 2.9
 */
@SuppressWarnings("serial")
public class InvalidDefinitionException
    extends JsonMappingException
{
    protected final JavaType _type;

    protected transient BeanDescription _beanDesc;
    protected transient BeanPropertyDefinition _property;

    protected InvalidDefinitionException(String msg, JavaType type) {
        super(msg);
        _type = type;
        _beanDesc = null;
        _property = null;
    }

    protected InvalidDefinitionException(String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        super(msg);
        _type = (bean == null) ? null : bean.getType();
        _beanDesc = bean;
        _property = prop;
    }

    public static InvalidDefinitionException from(String msg, JavaType type) {
        return new InvalidDefinitionException(msg, type);
    }

    public static InvalidDefinitionException from(String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        return new InvalidDefinitionException(msg, bean, prop);
    }

    /**
     * Accessor for type fully resolved type that had the problem; this should always
     * known and available, never <code>null</code>
     */
    public JavaType getType() {
        return _type;
    }

    /**
     * Accessor for type definition (class) that had the definition problem, if any; may sometimes
     * be undefined or unknown; if so, returns <code>null</code>.
     */
    public BeanDescription getBeanDescription() {
        return _beanDesc;
    }

    /**
     * Accessor for property that had the definition problem if any
     * (none, for example if the problem relates to type in general),
     * if known. If not known (or relevant), returns <code>null</code>.
     */
    public BeanPropertyDefinition getProperty() {
        return _property;
    }
}