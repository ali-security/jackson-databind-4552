package com.fasterxml.jackson.databind.jsontype;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.logging.FileHandler;

public class TestSubTypeValidator extends com.fasterxml.jackson.databind.BaseMapTest {
    public void testDeserializatioFails() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(FileHandler.class);
        try {
            mapper.readValue("{\"@type\":\"java.util.logging.FileHandler\"}", FileHandler.class);
            fail("Should not succeeed");
        } catch (JsonMappingException e) {
            verifyException(e, "Illegal type");
            verifyException(e, "prevented for security reasons");
        }
    }

}
