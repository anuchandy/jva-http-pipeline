package com.azjvsdk.experimental.http.pipeline;

import java.util.Objects;

/**
 * An entry holding policy and it's name.
 */
public class PolicyEntry {
    private final String name;
    private final RequestPolicy policy;

    /**
     * Creates PolicyEntry.
     *
     * @param name request policy name
     * @param policy request policy
     */
    public PolicyEntry(String name, RequestPolicy policy) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        //
        this.name = name;
        this.policy = policy;
    }

    /**
     * @return request policy name
     */
    public String name() {
        return this.name;
    }

    /**
     * @return request policy
     */
    public RequestPolicy policy() {
        return this.policy;
    }
}
