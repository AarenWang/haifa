package org.wrj.haifa.designpattern.orderpolicyrule.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Policy 输出结果：最终折扣 + 选择/拒绝明细。
 */
public final class PolicyResult {

    private final int appliedDiscountCents;
    private final List<DiscountCandidate> chosenCandidates;
    private final List<RejectedCandidate> rejectedCandidates;
    private final boolean capApplied;

    public PolicyResult(int appliedDiscountCents,
                        List<DiscountCandidate> chosenCandidates,
                        List<RejectedCandidate> rejectedCandidates,
                        boolean capApplied) {
        this.appliedDiscountCents = appliedDiscountCents;
        this.chosenCandidates = Collections.unmodifiableList(new ArrayList<>(chosenCandidates));
        this.rejectedCandidates = Collections.unmodifiableList(new ArrayList<>(rejectedCandidates));
        this.capApplied = capApplied;
    }

    public static PolicyResult empty() {
        return new PolicyResult(0, List.of(), List.of(), false);
    }

    public int getAppliedDiscountCents() {
        return appliedDiscountCents;
    }

    public List<DiscountCandidate> getChosenCandidates() {
        return chosenCandidates;
    }

    public List<RejectedCandidate> getRejectedCandidates() {
        return rejectedCandidates;
    }

    public boolean isCapApplied() {
        return capApplied;
    }
}
