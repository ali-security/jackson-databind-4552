package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

public class TestDeepNestingUntypedDeser extends BaseMapTest {
    private final static int NOT_TOO_DEEP = 1000;
    private final static int TOO_DEEP_NESTING = 1024;
    private final ObjectMapper mapper = new ObjectMapper();

    public void testFormerlyTooDeepUntypedWithArray() throws Exception {
        try {
            final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
            Object ob = mapper.readValue(doc, Object.class);
            fail("Should have thrown an exception.");
        } catch (JsonParseException jpe) {
            assertTrue(jpe.getMessage().startsWith("JSON is too deeply nested."));
        }
    }

    public void testTooDeepUntypedWithObject() throws Exception {
        try {
            final String doc = "{" + _nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
            Object ob = mapper.readValue(doc, Object.class);
            fail("Should have thrown an exception.");
        }catch (JsonParseException jpe) {
            assertTrue(jpe.getMessage().startsWith("JSON is too deeply nested."));
        }
    }

    public void testNotTooDeepArray() throws Exception {
        final String doc = _nestedDoc(NOT_TOO_DEEP, "[ ", "] ");
        Object ob = mapper.readValue(doc, Object.class);
        assertTrue(ob instanceof List<?>);    
    }

    public void testNotTooDeepObject() throws Exception {
        final String doc = "{" + _nestedDoc(NOT_TOO_DEEP, "\"x\":{", "} ") + "}";
        Object ob = mapper.readValue(doc, Object.class);
        assertTrue(ob instanceof Map<?, ?>);
    }

    private String _nestedDoc(int nesting, String open, String close) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}