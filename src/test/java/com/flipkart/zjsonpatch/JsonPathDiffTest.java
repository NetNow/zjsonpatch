package com.flipkart.zjsonpatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    /**
     * Test the standard JsonPathDiff mechanism
     * @throws Exception
     */
    @Test
    public void sampleJsonPathDiff_Test() throws Exception {
        ObjectNode before = getJsonNode("/testdata/hostSvcs_before.json");
        ObjectNode after = getJsonNode("/testdata/hostSvcs_after.json");
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> conf = new HashMap<String,String>();
        // conf.put(JsonPathDiff.DEFAULT_KEY, "name"); // Set a default fall-back key if none defined
        conf.put("$.list", "name");
        conf.put("$.list.services", "service_object_id");

        JsonNode actualPatch = JsonPathDiff.asJson(before, after, conf, true);
        String jsonStr = actualPatch.toString();
        String jsonPretty = getPrettyJson(jsonStr);
        System.out.println(jsonPretty);
        assertEquals(jsonStr,"[{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='bestraining5')]\"},{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='xenserver01')]\"},{\"op\":\"add\",\"path\":\"$.list.[?(@.name=='newserverNoServices')]\",\"value\":{\"icon\":\"server\",\"state\":\"up\",\"summary\":{\"ok\":\"1\",\"handled\":\"1\",\"computed_state\":\"ok\",\"unhandled\":\"0\",\"total\":\"1\"},\"unhandled\":\"0\",\"max_check_attempts\":\"2\",\"num_interfaces\":\"0\",\"state_duration\":\"1114952\",\"name\":\"newserverNoServices\",\"state_type\":\"hard\",\"current_check_attempt\":\"1\",\"output\":\"Host assumed UP - no results received\",\"num_services\":\"1\",\"downtime\":\"0\",\"last_check\":\"0\",\"alias\":\"\"}},{\"op\":\"remove\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='140')]\"},{\"op\":\"add\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='1401')]\",\"value\":{\"max_check_attempts\":\"3\",\"markdown\":\"0\",\"state_duration\":\"1468072\",\"state_type\":\"hard\",\"name\":\"Disk: /usr/local/nagios/var\",\"current_check_attempt\":\"1\",\"output\":\"DISK OK - free space: / 22698 MB (88% inode=95%): New Service\",\"state\":\"ok\",\"service_object_id\":\"1401\",\"unhandled\":\"0\",\"downtime\":\"0\",\"last_check\":\"1442401733\",\"perfdata_available\":\"1\"}},{\"op\":\"replace\",\"path\":\"$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='142')].output\",\"value\":\"DISK ERROR\"},{\"op\":\"replace\",\"path\":\"$.list.[?(@.name=='10.0.0.137')].state\",\"value\":\"down\"}]");
    }
    
    /**
     * Test the comparison of 2 objects, with record keys. The output is keyed using a JsonPath 
     * reference to the source or target (see JsonChanges object).
     * @throws Exception
     */
    @Test
    public void sampleJsonPathDiffSummary_Test() throws Exception {
        ObjectNode before = getJsonNode("/testdata/hostSvcs_before.json");
        ObjectNode after = getJsonNode("/testdata/hostSvcs_after.json");
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> conf = new HashMap<String,String>();
        // conf.put(JsonPathDiff.DEFAULT_KEY, "name");
        conf.put("$.list", "name");
        conf.put("$.list.services", "service_object_id");

        List<String> listenPaths = new ArrayList<String>();
        listenPaths.add("$.list.state");
        listenPaths.add("$.list.services.state");
        
        JsonChanges changes = JsonPathDiff.asSummary(before, after, conf, true, listenPaths);
        Map<String,Operation> changeMap = changes.getChangeMap();
        
        Set<String> keys = changeMap.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Operation op = changeMap.get(key);
            System.out.println("key = " + key + "\nop = " + op);
        }
        
        assertTrue(changeMap.get("$.list.[?(@.name=='bestraining5')]") == Operation.REMOVE);
        assertTrue(changeMap.get("$.list.[?(@.name=='xenserver01')]") == Operation.REMOVE);
        assertTrue(changeMap.get("$.list.[?(@.name=='newserverNoServices')]") == Operation.ADD);
        assertTrue(changeMap.get("$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='140')]") == Operation.REMOVE);
        assertTrue(changeMap.get("$.list.[?(@.name=='opsview')].services.[?(@.service_object_id=='1401')]") == Operation.ADD);
        assertTrue(changeMap.get("$.list.[?(@.name=='10.0.0.137')].state") == Operation.REPLACE);   
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
