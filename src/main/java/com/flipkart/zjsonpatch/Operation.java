package com.flipkart.zjsonpatch;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
public enum Operation {
    ADD("add", 1),
    REMOVE("remove", 2),
    REPLACE("replace", 4),
    MOVE("move", 8);

    private final static Map<String, Operation> OPS = ImmutableMap.of(
            ADD.rfcName, ADD,
            REMOVE.rfcName, REMOVE,
            REPLACE.rfcName, REPLACE,
            MOVE.rfcName, MOVE
            );

    private String rfcName;
    private int value;

    Operation(String rfcName, int value) {
        this.rfcName = rfcName;
        this.value = value;
    }

    public static Operation fromRfcName(String rfcName) {
        checkNotNull(rfcName, "rfcName cannot be null");
        return checkNotNull(OPS.get(rfcName.toLowerCase()), "unknown / unsupported operation %s", rfcName);
    }

    public String rfcName() {
        return this.rfcName;
    }
    
    public int value() {
        return this.value;
    }

}
