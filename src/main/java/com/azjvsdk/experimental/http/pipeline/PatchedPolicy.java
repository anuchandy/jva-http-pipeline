package com.azjvsdk.experimental.http.pipeline;

/**
 * Type representing the desired patch operation on a policy.
 */
public class PatchedPolicy {
    public final PolicyPatchAction action;
    public final PolicyEntry[] policyEntries;
    public final static PatchedPolicy NOP = new PatchedPolicy(PolicyPatchAction.NOP, null, null);

    /**
     * Creates PatchedPolicy.
     *
     * @param action the desired action
     * @param policyName policy name
     * @param policyEntries new policies to be added
     */
    public PatchedPolicy(PolicyPatchAction action, String policyName, PolicyEntry[] policyEntries) {
        this.action = action;
        this.policyEntries = policyEntries;
    }
}
