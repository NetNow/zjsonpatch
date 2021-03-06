package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
class Diff {
    private Operation operation;
    private List<Object> path;
    private JsonNode value;
    private List<Object> toPath; //only to be used in move operation
    private String keyPath;
    
    Diff(Operation operation, List<Object> path, JsonNode value) {
        this.operation = operation;
        this.path = path;
        this.value = value;
    }
    
    Diff(Operation operation, List<Object> path, JsonNode value, String keyPath) {
        this.operation = operation;
        this.path = path;
        this.value = value;
        this.keyPath = keyPath;
    }

    Diff(Operation operation, List<Object> fromPath, JsonNode value, List<Object> toPath) {
        this.operation = operation;
        this.path = fromPath;
        this.value = value;
        this.toPath = toPath;
    }

    public Operation getOperation() {
        return operation;
    }

    public List<Object> getPath() {
        return path;
    }

    public String getKeyPath() {
        return keyPath;
    }
    
    public JsonNode getValue() {
        return value;
    }
    
    public static Diff generateDiff(Operation replace, List<Object> path, JsonNode target) {
        return new Diff(replace, path, target);
    }
    
//    public static Diff generateDiff(Operation replace, List<Object> path, JsonNode target) {
//        return new Diff(replace, path, target, keyPath);
//    }

    List<Object> getToPath() {
        return toPath;
    }
}
