package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class JsonChanges {

    private List<String> added;
    private List<String> removed;
    private List<String> replaced;
    private JsonNode source;
    private JsonNode target;
    private Map<String,Operation> changeMap;

    public JsonChanges(List<String> added, List<String> removed, List<String> replaced) {
        this.added = added;
        this.removed = removed;
        this.replaced = replaced;
    }
    
    public JsonChanges(Map<String,Operation> changeMap) {
        this.changeMap = changeMap;
    }
    
    public Map<String, Operation> getChangeMap() {
        return changeMap;
    }

    public void setChangeMap(Map<String, Operation> changeMap) {
        this.changeMap = changeMap;
    }

    public void setAdded(List<String> added) {
        this.added = added;
    }

    public void setRemoved(List<String> removed) {
        this.removed = removed;
    }

    public void setReplaced(List<String> replaced) {
        this.replaced = replaced;
    }
        
    public List<String> getAdded() {
        return added;
    }
    
    public List<String> getRemoved() {
        return removed;
    }
    
    public List<String> getReplaced() {
        return replaced;
    }

    public void setSource(JsonNode source) {
        this.source = source;
    }

    public void setTarget(JsonNode target) {
        this.target = target;
    }
    
    public JsonNode getSource() {
        return source;
    }

    public JsonNode getTarget() {
        return target;
    }    
}
