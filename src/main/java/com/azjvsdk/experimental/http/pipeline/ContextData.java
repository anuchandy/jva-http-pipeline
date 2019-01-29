package com.azjvsdk.experimental.http.pipeline;

import java.util.Optional;

/**
 * {@code ContextData} offers a means of passing arbitrary data (key/value pairs) to an {@link HttpPipeline}'s
 * policy objects. Most applications do not need to pass arbitrary data to the pipeline and can pass
 * {@code Context.NONE} or {@code null}. Each context object is immutable. The {@link this#addData(Object, Object)}
 * method creates a new {@code ContextData} object that refers to its parent, forming a linked list.
 */
public class ContextData {
    // All fields must be immutable.

    /**
     * Signifies that no data need be passed to the pipeline.
     */
    public static final ContextData NONE = new ContextData(null, null, null);

    private final ContextData parent;
    private final Object key;
    private final Object value;

    /**
     * Constructs a new {@link ContextData} object.
     *
     * @param key
     *      The key.
     * @param value
     *      The value.
     */
    public ContextData(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        this.parent = null;
        this.key = key;
        this.value = value;
    }

    private ContextData(ContextData parent, Object key, Object value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    /**
     * Adds a new immutable {@link ContextData} object with the specified key/value pair to the existing {@link ContextData}
     * chain.
     *
     * @param key
     *      The key.
     * @param value
     *      The value.
     * @return
     *      The new {@link ContextData} object containing the specified pair added to the set of pairs.
     */
    public ContextData addData(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        return new ContextData(this, key, value);
    }

    /**
     * Scans a linked-list of {@link ContextData} objects looking for one with the specified key. Note that the first key
     * found, i.e. the most recently added, will be returned.
     *
     * @param key
     *      The key to search for.
     * @return
     *      The value of the key if it exists.
     */
    public Optional<Object> getData(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        for (ContextData c = this; c != null; c = c.parent) {
            if (key.equals(c.key)) {
                return Optional.of(c.value);
            }
        }
        return Optional.empty();
    }
}