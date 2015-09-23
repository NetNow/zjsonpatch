package com.flipkart.zjsonpatch;

import static org.junit.Assert.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class JsonPatchDiff {
    
    static ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode getJsonNode(String path) throws JsonProcessingException, IOException {
        InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(testData);
        return jsonNode;
    }
    
    public static String getPrettyJson(String jsonStr) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Object json = mapper.readValue(jsonStr, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
    
    @Test
    public void testSampleJsonPathDiff() throws Exception {
        
        ObjectNode before = getJsonNode("/testdata/hostSvcs_before.json");
        ObjectNode after = getJsonNode("/testdata/hostSvcs_after.json");

        Map<String,String> conf = new HashMap<String,String>();
        //conf.put("$.list", "name");
        conf.put(JsonPathDiff.DEFAULT_KEY, "name");
        conf.put("$.list.services", "service_object_id");
        
        JsonNode actualPatch = JsonPathDiff.asJson(before, after, conf, true);    
        // System.out.println("out = " + prettyJson);
        
        System.out.println(getPrettyJson(actualPatch.toString()));
        assertEquals(actualPatch.toString(),"[{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='bestraining5')]\"},{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='xenserver01')]\"},{\"op\":\"add\",\"path\":\"$.list.[?(@.name=='newserverNoServices')]\",\"value\":{\"icon\":\"server\",\"state\":\"up\",\"summary\":{\"ok\":\"1\",\"handled\":\"1\",\"computed_state\":\"ok\",\"unhandled\":\"0\",\"total\":\"1\"},\"unhandled\":\"0\",\"max_check_attempts\":\"2\",\"num_interfaces\":\"0\",\"state_duration\":\"1114952\",\"name\":\"newserverNoServices\",\"state_type\":\"hard\",\"current_check_attempt\":\"1\",\"output\":\"Host assumed UP - no results received\",\"num_services\":\"1\",\"downtime\":\"0\",\"last_check\":\"0\",\"alias\":\"\"}},{\"op\":\"remove\",\"path\":\"$.list.services.[?(@.service_object_id=='140')]\"},{\"op\":\"add\",\"path\":\"$.list.services.[?(@.service_object_id=='1401')]\",\"value\":{\"max_check_attempts\":\"3\",\"markdown\":\"0\",\"state_duration\":\"1468072\",\"state_type\":\"hard\",\"name\":\"Disk: /usr/local/nagios/var\",\"current_check_attempt\":\"1\",\"output\":\"DISK OK - free space: / 22698 MB (88% inode=95%): New Service\",\"state\":\"ok\",\"service_object_id\":\"1401\",\"unhandled\":\"0\",\"downtime\":\"0\",\"last_check\":\"1442401733\",\"perfdata_available\":\"1\"}}]");
    }
}
