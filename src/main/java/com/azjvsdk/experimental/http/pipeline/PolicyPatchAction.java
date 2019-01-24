package com.azjvsdk.experimental.http.pipeline;

/**
 * Patch action to perform per policy.
 */
public enum PolicyPatchAction {
    // Remove the current policy.
    REMOVE,
    // Replace the current policy with new policy.
    REPLACE,
    // Add a new policy after the current policy.
    ADD_AFTER,
    // Add a new policy before the current policy.
    ADD_BEFORE,
    // Keep the current policy as it is.
    NOP
}
