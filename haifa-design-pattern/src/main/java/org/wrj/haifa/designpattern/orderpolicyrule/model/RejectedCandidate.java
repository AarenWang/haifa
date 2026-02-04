package org.wrj.haifa.designpattern.orderpolicyrule.model;

import java.util.Objects;

/**
 * 被拒绝的候选折扣（带原因）。
 */
public final class RejectedCandidate {

    private final DiscountCandidate candidate;
    private final String reason;

    public RejectedCandidate(DiscountCandidate candidate, String reason) {
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public DiscountCandidate getCandidate() {
        return candidate;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "RejectedCandidate{" +
                "candidate=" + candidate +
                ", reason='" + reason + '\'' +
                '}';
    }
}
