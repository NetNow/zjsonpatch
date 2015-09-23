package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Generate JSONPath compatible paths to figure out what's changed.
// Note this is not compatible with this libraries JsonPatch.apply
public class JsonPathDiff {
    private static Logger log = LoggerFactory.getLogger(JsonPathDiff.class);
    
    public static final String DEFAULT_KEY = "DEFAULT";
    public static final EncodePathFunction ENCODE_PATH_FUNCTION = new EncodePathFunction();

    private final static class EncodePathFunction implements Function<Object, String> {
        @Override
        public String apply(Object object) {
            String path = object.toString(); // see http://tools.ietf.org/html/rfc6901#section-4
            return path;
           // return path.replaceAll("~", "~0").replaceAll("/", "~1");
        }
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target, Map config, boolean setChangedValue) throws Exception {
        final List<Diff> diffs = new ArrayList<Diff>();
        List<Object> path = new LinkedList<Object>();
        /**
         * generating diffs in the order of their occurrence
         */
        generateDiffs(diffs, path, source, target, config);
        
        return getJsonNodes(diffs, setChangedValue);
    }

    private static ArrayNode getJsonNodes(List<Diff> diffs, boolean setChangedValue) {
        JsonNodeFactory FACTORY = JsonNodeFactory.instance;
        final ArrayNode patch = FACTORY.arrayNode();
        for (Diff diff : diffs) {
            ObjectNode jsonNode = getJsonNode(FACTORY, diff, setChangedValue);
            patch.add(jsonNode);
        }
        return patch;
    }

    private static ObjectNode getJsonNode(JsonNodeFactory FACTORY, Diff diff, boolean setChangedValue) {
        ObjectNode jsonNode = FACTORY.objectNode();
        jsonNode.put(Constants.OP, diff.getOperation().rfcName());
        jsonNode.put(Constants.PATH, getArrayNodeRepresentation(diff.getPath()));
        if (Operation.MOVE.equals(diff.getOperation())) {
            jsonNode.put(Constants.FROM, getArrayNodeRepresentation(diff.getPath())); //required {from} only in case of Move Operation
            jsonNode.put(Constants.PATH, getArrayNodeRepresentation(diff.getToPath()));  // destination Path
        }
        if (setChangedValue && !Operation.REMOVE.equals(diff.getOperation()) && !Operation.MOVE.equals(diff.getOperation())) { // setting only for Non-Remove operation
            jsonNode.put(Constants.VALUE, diff.getValue());
        }
        return jsonNode;
    }

    private static String getArrayNodeRepresentation(List<Object> path) {
        StringBuilder str = new StringBuilder();
        for (int i=0; i< path.size(); i++) {
            if (i == 0) {
                str.append("$.");
            } else if (i < path.size()) {
                str.append(".");
            }
            str.append(path.get(i));
        }
        return str.toString();
    }

    private static void generateDiffs(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target, Map config) throws Exception {
        if (!source.equals(target)) {
            final NodeType sourceType = NodeType.getNodeType(source);
            final NodeType targetType = NodeType.getNodeType(target);

            if (sourceType == NodeType.ARRAY && targetType == NodeType.ARRAY) {
                //both are arrays
                compareArray(diffs, path, source, target, config);
            } else if (sourceType == NodeType.OBJECT && targetType == NodeType.OBJECT) {
                //both are json
                compareObjects(diffs, path, source, target, config);
            } else {
                //can be replaced
                diffs.add(Diff.generateDiff(Operation.REPLACE, path, target));
            }
        }
    }    

    private static void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target, Map config) throws Exception {
       
        String arrKey = getArrayKey(config, path);
        Map<String,JsonNode> mapSource = jsonArrayToMap(config, arrKey, source);
        Map<String,JsonNode> mapTarget = jsonArrayToMap(config, arrKey, target);
        
        Collection<String> removed = CollectionUtils.subtract(mapSource.keySet(), mapTarget.keySet());
        Collection<String> added = CollectionUtils.subtract(mapTarget.keySet(), mapSource.keySet());
        
        for (String key : removed) {
            List<Object> currPath = JsonDiff.getPath(path, getArrayPath(arrKey,key));
            JsonNode srcNode = (JsonNode)mapSource.get(key);
            diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode));
        }
        
        for (String key : added) {
            List<Object> currPath = JsonDiff.getPath(path, getArrayPath(arrKey,key));
            JsonNode targetNode = (JsonNode)mapTarget.get(key);
            diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode));
        }
        
        Collection<String> remaining = CollectionUtils.subtract(mapTarget.keySet(),added);
        for (String key : remaining) {
            JsonNode src = mapSource.get(key);
            JsonNode targ = mapTarget.get(key);
            generateDiffs(diffs, path, src, targ, config);
        }
    }

    private static Object getArrayPath(String arrKey, String key) {
        StringBuilder str = new StringBuilder(arrKey.length() + key.length() + 16);
        str.append("[?(@.");
        str.append(arrKey);
        str.append("=='");
        str.append(key);
        str.append("')]");
        return str.toString();
    }

    private static Map<String,JsonNode> jsonArrayToMap(Map config, String arrKey, JsonNode source) throws Exception {
        Map<String,JsonNode> map = new HashMap<String,JsonNode>();
        Iterator<JsonNode> it = source.iterator();
        while (it.hasNext()) {
            JsonNode jn = it.next();
            JsonNode keyNode = jn.get(arrKey);
            String key = null;
            if (keyNode == null) {
                throw new Exception("No key '" + arrKey + "' defined for JSONArray: " + source);
            }
            key = keyNode.asText();
            map.put(key, jn);
        }
        return map;
    }

    private static String getArrayKey(Map<String,String> config, List<Object> path) throws Exception {
        String currentPath = getArrayNodeRepresentation(path);
        String key = null;
        if (!config.containsKey(currentPath)) {
            if ((key = (String)config.get(DEFAULT_KEY)) == null) {
                throw new Exception("No key defined for path and no " + DEFAULT_KEY + " defined: " + currentPath);
            } else {
                return key;
            }
        }
        return (String)config.get(currentPath);
    }

    private static void compareObjects(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target, Map config) throws Exception {
        Iterator<String> keysFromSrc = source.fieldNames();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next();
            if (!target.has(key)) {
                //remove case
                List<Object> currPath = JsonDiff.getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = JsonDiff.getPath(path, key);
            generateDiffs(diffs, currPath, source.get(key), target.get(key), config);
        }
        Iterator<String> keysFromTarget = target.fieldNames();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next();
            if (!source.has(key)) {
                //add case
                List<Object> currPath = JsonDiff.getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.ADD, currPath, target.get(key)));
            }
        }
    }
}
