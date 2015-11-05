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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate JSONPath compatible paths to figure out what's changed.
 * Note this is not compatible with this libraries JsonPatch.apply
 */
public class JsonPathDiff {
    private static Logger log = LoggerFactory.getLogger(JsonPathDiff.class);
    
    public static final String DEFAULT_KEY = "DEFAULT";
    public static final EncodePathFunction ENCODE_PATH_FUNCTION = new EncodePathFunction();
    //private static ExecutorService executor = Executors.newScheduledThreadPool(20);
   
    private final static class EncodePathFunction implements Function<Object, String> {
        @Override
        public String apply(Object object) {
            String path = object.toString();
            return path;
        }
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target, Map config, boolean setChangedValue) throws Exception {
        final List<Diff> diffs = new ArrayList<Diff>();
        LinkedList<Object> path = new LinkedList<Object>();
        LinkedList<Object> pathStack = new LinkedList<Object>();
        /**
         * generating diffs in the order of their occurrence
         */
        
        generateDiffs(diffs, path, pathStack, source, target, config, null);
        
        return getJsonNodes(diffs, setChangedValue);
    }
    
    public static JsonChanges asSummary(final JsonNode source, final JsonNode target, 
                                        Map config, boolean setChangedValue, List<String> listenPaths) throws Exception {
        final List<Diff> diffs = new ArrayList<Diff>();
        LinkedList<Object> path = new LinkedList<Object>();
        LinkedList<Object> pathStack = new LinkedList<Object>();
        /**
         * generating diffs in the order of their occurrence
         */
        
        generateDiffs(diffs, path, pathStack, source, target, config, listenPaths);
        JsonChanges changes = getJsonChangesByPath(diffs);
        changes.setSource(source);
        changes.setTarget(target);
        
        return changes;
    }
    
    /**
     * Generate an object containing 3 separate lists of diffs
     * @param diffs
     * @return
     */
    public static JsonChanges getJsonChanges(List<Diff> diffs) {
        
        List<String> added = new ArrayList<String>();
        List<String> removed = new ArrayList<String>();
        List<String> replaced = new ArrayList<String>();
        
        for (Diff diff : diffs) {
            Operation op = diff.getOperation();
            String path = getArrayNodeRepresentation(diff.getPath());
            if (op == Operation.ADD) {
                added.add(path);
            } else if (op == Operation.REMOVE) {
                removed.add(path);
            } else if (op == Operation.REPLACE) {
                replaced.add(path);
            }
        }
        JsonChanges changes = new JsonChanges(added, removed, replaced);
        return changes;
    }
    
    /**
     * Get the changes by path
     * @param diffs
     * @return
     * @throws Exception
     */
    public static JsonChanges getJsonChangesByPath(List<Diff> diffs) throws Exception {
        
        Map<String,Operation> changeMap = new LinkedHashMap<String,Operation>();
        Operation op = null;
        for (Diff diff : diffs) {
            String path = getArrayNodeRepresentation(diff.getPath());
            if ((op=changeMap.get(path)) != null) {
                throw new Exception("Path is not unique in map! " + path);
            }
            op = diff.getOperation();
            changeMap.put(path, op);
        }
        JsonChanges changes = new JsonChanges(changeMap);
        return changes;
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
        
//        if (diff.getKeyPath() != null) {
//            jsonNode.put(Constants.KEY, diff.getKeyPath());
//        }
//        if (Operation.MOVE.equals(diff.getOperation())) {
//            jsonNode.put(Constants.FROM, getArrayNodeRepresentation(diff.getPath())); //required {from} only in case of Move Operation
//            jsonNode.put(Constants.PATH, getArrayNodeRepresentation(diff.getToPath()));  // destination Path
//        }
        if (setChangedValue && !Operation.REMOVE.equals(diff.getOperation()) 
            && !Operation.MOVE.equals(diff.getOperation())) { // setting only for Non-Remove operation
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

    private static void generateDiffs(List<Diff> diffs, List<Object> path, LinkedList<Object> pathStack, 
                                     JsonNode source, JsonNode target, Map config, List<String> listenPaths) throws Exception {
        if (source == null) {
            diffs.add(Diff.generateDiff(Operation.ADD, getPath(path,""), target));
            return;
        }
        if (!source.equals(target) && validPath(pathStack, listenPaths)) {
            final NodeType sourceType = NodeType.getNodeType(source);
            final NodeType targetType = NodeType.getNodeType(target);
            
            if (sourceType == NodeType.ARRAY && targetType == NodeType.ARRAY) {
                //both are arrays
                compareArray(diffs, path, pathStack, source, target, config, listenPaths);
            } else if (sourceType == NodeType.OBJECT && targetType == NodeType.OBJECT) {
                //both are json
                compareObjects(diffs, path, pathStack, source, target, config, listenPaths);
            } else {
                //can be replaced
                diffs.add(Diff.generateDiff(Operation.REPLACE, path, target));
            }
        }
    }    

    private static boolean validPath(LinkedList<Object> pathStack, List<String> listenPaths) {
        if (listenPaths == null) {
            return true;
        }
        
        String currentPath = getPathRepresentation(pathStack);
        for (String path: listenPaths) {
            log.debug("listenPath/current: " + path + " " + currentPath);
            if (path.startsWith(currentPath)) {
                return true;
            }
        }
        return false;
    }

    private static void compareArray(List<Diff> diffs, List<Object> path, 
                                     LinkedList<Object> pathStack, 
                                     JsonNode source, JsonNode target, 
                                     Map config, List<String> listenPaths) throws Exception {
        //log.debug("PathCurrent = " + path.get(path.size()-1));
        String arrKey = getArrayKey(config, pathStack);
        Map<String,JsonNode> mapSource = jsonArrayToMap(config, arrKey, source);
        Map<String,JsonNode> mapTarget =  jsonArrayToMap(config, arrKey, target);
       
        if (mapSource.size() == 0) {
            List<Object> currPath = getPath(path, null);
            diffs.add(Diff.generateDiff(Operation.ADD, currPath, target));
            return;
        }
        
        Collection<String> removed = CollectionUtils.subtract(mapSource.keySet(), mapTarget.keySet());
       
        for (String key : removed) {
            List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
            JsonNode srcNode = mapSource.get(key);
            diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode));
        }

        final Collection<String> added = CollectionUtils.subtract(mapTarget.keySet(), mapSource.keySet());
      
        for (String key : added) {
            List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
            JsonNode targetNode = mapTarget.get(key);
            diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode));
        }     
                        
        Collection<String> remaining = CollectionUtils.subtract(mapTarget.keySet(),added);
        for (String key : remaining) {
            JsonNode src = mapSource.get(key);
            JsonNode targ = mapTarget.get(key);
            List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
            generateDiffs(diffs, currPath, pathStack, src, targ, config, listenPaths);
        }
    }
    
    // TODO Threaded version of compareArray (must ensure sync list is used), but SLOWER than single thread version above. 
    // May be useful with huge datasets later on
//    private static void compareArray(final List<Diff> diffs, final List<Object> path, 
//                                     final LinkedList<Object> pathStack, 
//                                     final JsonNode source, final JsonNode target, 
//                                     final Map config) throws Exception {
//
//        
//        //log.debug("PathCurrent = " + path.get(path.size()-1));
//        final String arrKey = getArrayKey(config, pathStack);
//        Future<Map<String,JsonNode>> mapSourceFuture = executor.submit(new Callable<Map<String,JsonNode>>()
//        {
//            @Override
//            public Map<String,JsonNode> call() throws Exception
//            {
//                return jsonArrayToMap(config, arrKey, source);
//            }
//        });
//        Future<Map<String,JsonNode>> mapTargetFuture = executor.submit(new Callable<Map<String,JsonNode>>()
//           {
//               @Override
//               public Map<String,JsonNode> call() throws Exception
//               {
//                   return jsonArrayToMap(config, arrKey, target);
//               }
//        });
//        
//        final Map<String,JsonNode> mapSource = mapSourceFuture.get();
//        final Map<String,JsonNode> mapTarget = mapTargetFuture.get();
//        
//        Future<List<Diff>> futureRemoved = executor.submit(new Callable<List<Diff>>()
//        {
//            @Override
//            public List<Diff> call() throws Exception
//            {
//                Collection<String> removed = CollectionUtils.subtract(mapSource.keySet(), mapTarget.keySet());
//               // List<Diff> diffsOut = new ArrayList<Diff>();
//                for (String key : removed) {
//                    List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
//                    JsonNode srcNode = mapSource.get(key);
//                    diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode));
//                }
//                return null;
//            }
//        });
//        
//       
//        Future<List<Diff>> futureAdded = executor.submit(new Callable<List<Diff>>()
//        {
//            @Override
//            public List<Diff> call() throws Exception
//            {
//                final Collection<String> added = CollectionUtils.subtract(mapTarget.keySet(), mapSource.keySet());
//                
//                Future<List<Diff>> futureAdding = executor.submit(new Callable<List<Diff>>()
//                {
//                    @Override
//                    public List<Diff> call() throws Exception {
//                       // List<Diff> diffsAdded = new ArrayList<Diff>();
//                        for (String key : added) {
//                            List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
//                            JsonNode targetNode = mapTarget.get(key);
//                            diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode));
//                        }
//                        return null;
//                    }
//                    
//                });
//                                
//                Collection<String> remaining = CollectionUtils.subtract(mapTarget.keySet(),added);
//
//                for (String key : remaining) {
//                    JsonNode src = mapSource.get(key);
//                    JsonNode targ = mapTarget.get(key);
//                    List<Object> currPath = getPath(path, getArrayPath(arrKey,key));
//                    generateDiffs(diffs, currPath, pathStack, src, targ, config);
//                }
//                futureAdding.get();
//                return null;
//            }
//        });
//        
//        futureRemoved.get();
//        futureAdded.get();
//    }

    static List<Object> getPath(List<Object> path, Object key) {
        List<Object> newList = new ArrayList<Object>(path);
        if (key != null) {
            newList.add(key);
        }
        return newList;
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

    private static String getArrayKey(Map<String,String> config, LinkedList<Object> path) throws Exception {
        String currentPath = getPathRepresentation(path);
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

    private static String getPathRepresentation(LinkedList<Object> path) {
        Iterator<Object> it = path.descendingIterator();
        StringBuilder str = new StringBuilder();
        str.append("$.");
        
        while (it.hasNext()) {
            str.append(it.next());
            if (it.hasNext()) {
                str.append(".");
            }
        }
        //log.debug("Path = " + str.toString());
        return str.toString();
    }

    private static void compareObjects(List<Diff> diffs, List<Object> path, LinkedList<Object> pathStack, 
                                       JsonNode source, JsonNode target, Map config, List<String> listenPaths) throws Exception {
        
        Iterator<String> keysFromSrc = source.fieldNames();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next();
            if (!target.has(key)) {
                //remove case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = getPath(path, key);
            pathStack.push(key);
            generateDiffs(diffs, currPath, pathStack, source.get(key), target.get(key), config, listenPaths);
            pathStack.pop();
        }
        Iterator<String> keysFromTarget = target.fieldNames();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next();
            if (!source.has(key)) {
                //add case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.ADD, currPath, target.get(key)));
            }
        }
    }
}
