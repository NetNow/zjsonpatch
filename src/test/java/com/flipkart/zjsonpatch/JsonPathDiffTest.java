package com.flipkart.zjsonpatch;

import static org.junit.Assert.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class JsonPathDiffTest {
    
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
    public void sampleJsonPathDiff_Test() throws Exception {
        ObjectNode before = getJsonNode("/testdata/hostSvcs_before.json");
        ObjectNode after = getJsonNode("/testdata/hostSvcs_after.json");

        Map<String,String> conf = new HashMap<String,String>();
        // conf.put(JsonPathDiff.DEFAULT_KEY, "name");
        conf.put("$.list", "name");
        conf.put("$.list.services", "service_object_id");

        JsonNode actualPatch = JsonPathDiff.asJson(before, after, conf, true);
        String jsonStr = actualPatch.toString();
        String jsonPretty = getPrettyJson(jsonStr);
        System.out.println(jsonPretty);
        assertEquals(jsonStr,"[{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='bestraining5')]\"},{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='xenserver01')]\"},{\"op\":\"add\",\"path\":\"$.list.[?(@.name=='newserverNoServices')]\",\"value\":{\"icon\":\"server\",\"state\":\"up\",\"summary\":{\"ok\":\"1\",\"handled\":\"1\",\"computed_state\":\"ok\",\"unhandled\":\"0\",\"total\":\"1\"},\"unhandled\":\"0\",\"max_check_attempts\":\"2\",\"num_interfaces\":\"0\",\"state_duration\":\"1114952\",\"name\":\"newserverNoServices\",\"state_type\":\"hard\",\"current_check_attempt\":\"1\",\"output\":\"Host assumed UP - no results received\",\"num_services\":\"1\",\"downtime\":\"0\",\"last_check\":\"0\",\"alias\":\"\"}},{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='140')]\"},{\"op\":\"add\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='1401')]\",\"value\":{\"max_check_attempts\":\"3\",\"markdown\":\"0\",\"state_duration\":\"1468072\",\"state_type\":\"hard\",\"name\":\"Disk: /usr/local/nagios/var\",\"current_check_attempt\":\"1\",\"output\":\"DISK OK - free space: / 22698 MB (88% inode=95%): New Service\",\"state\":\"ok\",\"service_object_id\":\"1401\",\"unhandled\":\"0\",\"downtime\":\"0\",\"last_check\":\"1442401733\",\"perfdata_available\":\"1\"}},{\"op\":\"replace\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='142')].output\",\"value\":\"DISK ERROR\"}]");
        
    }
    
   // @Test
//    public void sampleJsonPathDiff_SpeedTest() throws Exception {
//        System.out.println("Load before");
//        ObjectNode before = getJsonNode("/testdata/hostSvcs_bf.json");
//        ObjectNode after = getJsonNode("/testdata/hostSvcs_bf-2409.json");
//        System.out.println("Load after");
//
//        Map<String,String> conf = new HashMap<String,String>();
//        conf.put("$.list", "name");
//        conf.put("$.list.services", "service_object_id");
//        
//        long count = 0;
//        long timeBefore = System.currentTimeMillis();
//        
//        int repeat = 20;
//        while (count < repeat) {
//            //System.out.println("Compare");
//            JsonNode actualPatch = JsonPathDiff.asJson(before, after, conf, true);
//            String patchStr = actualPatch.toString();
//            Thread.sleep(100);
//            count++;
//            if (count % 5 == 0) {
//                System.out.println("Count: " + count);
//            }
//        }
//        long timeAfter = System.currentTimeMillis();
//        long timeTaken = (timeAfter-timeBefore);
//        long ttPerIteration = (timeTaken / repeat);
//        System.out.println("TimeTaken = " + timeTaken + "ms" + " per iteration: " + ttPerIteration + "ms");
//    }
}
