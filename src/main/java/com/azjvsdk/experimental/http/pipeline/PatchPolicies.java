package com.azjvsdk.experimental.http.pipeline;

/**
 * Defines a set of callback methods to support patching of policies.
 */
public interface PatchPolicies {

    /**
     * The method that gets called with each policy.
     * Implementation can inspect given policy and express the desired patch
     * action on the policy through returned {@link PatchedPolicy}.
     *
     * @param name name of next policy
     * @param policy the next policy
     * @return the patch to be applied on the given policy
     */
    default PatchedPolicy onNext(String name, RequestPolicy policy) {
        return PatchedPolicy.NOP;
    }

    /**
     * The method that gets called when desired policy patch action
     * expressed as the return value of last {@link this#onNext(String, RequestPolicy)}
     * cannot be applied.
     *
     * @param reason the reason for unable to apply the patch
     * @param name the name of the policy that failed to patch
     */
    default void onError(PolicyPatchFailed reason, String name) { }

    /**
     * @return the ordered array of policies to prepend to current policies.
     */
    default PolicyEntry[] headEntries() {
        return null;
    }

    /**
     * @return the ordered array of policies to append to current policies.
     */
    default PolicyEntry[] tailEntries() {
        return null;
    }

    /**
     * @return the method that gets after a full pass to decide whether
     * another pass is needed.
     */
    default boolean requireRerun() {
        return false;
    }
}
