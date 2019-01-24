package com.azjvsdk.experimental.http.pipeline;

import java.util.Objects;

/**
 * Package private type representing a node in the list, where each
 * node holds policy name, reference to the policy and reference to
 * next node.
 */
class PolicyRef {
    final String name;
    final RequestPolicy policy;
    PolicyRef previous;
    PolicyRef next;

    PolicyRef(PolicyEntry nameAndPolicy) {
        Objects.requireNonNull(nameAndPolicy);
        this.name = Objects.requireNonNull(nameAndPolicy.name());
        this.policy = Objects.requireNonNull(nameAndPolicy.policy());
    }

    PolicyRef(String name, RequestPolicy policy) {
        this.name = Objects.requireNonNull(name);
        this.policy = Objects.requireNonNull(policy);
    }

    static void addAfter(PolicyRef newItem, PolicyRef existingItem) {
        Objects.requireNonNull(newItem);
        Objects.requireNonNull(existingItem);
        newItem.next = existingItem.next;
        if (newItem.next != null) {
            newItem.next.previous = newItem;
        }
        newItem.previous = existingItem;
        existingItem.next = newItem;
    }

    static void addBefore(PolicyRef newItem, PolicyRef existingItem) {
        Objects.requireNonNull(newItem);
        Objects.requireNonNull(existingItem);
        newItem.previous = existingItem.previous;
        if (newItem.previous != null) {
            newItem.previous.next = newItem;
        }
        newItem.next = existingItem;
        existingItem.previous = newItem;
    }

    static void replace(PolicyRef newItem, PolicyRef oldItem) {
        Objects.requireNonNull(newItem);
        Objects.requireNonNull(oldItem);
        newItem.next = oldItem.next;
        newItem.previous = oldItem.previous;
        if (oldItem.previous != null) {
            oldItem.previous.next = newItem;
        }
        if (oldItem.next != null) {
            oldItem.next.previous = newItem;
        }
    }

    static void remove(PolicyRef item) {
        if (item.previous != null) {
            item.previous.next = item.next;
        }
        if (item.next != null) {
            item.next.previous = item.previous;
        }
    }
}

