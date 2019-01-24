package com.azjvsdk.experimental.http.pipeline;

/**
 * Reason for patch operation failure.
 */
public enum PolicyPatchFailed {
    // Indicate that there is a conflict in policy name.
    NAME_CONFLICT,
}
