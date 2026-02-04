package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.wrj.haifa.designpattern.orderpolicyrule.model.ConflictMatrix;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import org.wrj.haifa.designpattern.orderpolicyrule.model.RejectedCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 在基础 Policy 上叠加跨组冲突矩阵。
 */
public class ConflictMatrixPolicy implements DiscountPolicy {

    private static final Comparator<DiscountCandidate> BEST_FIRST =
            Comparator.comparingInt(DiscountCandidate::getDiscountCents)
                    .thenComparingInt(DiscountCandidate::getPriority)
                    .reversed();

    private final DiscountPolicy basePolicy;
    private final ConflictMatrix matrix;

    public ConflictMatrixPolicy(DiscountPolicy basePolicy, ConflictMatrix matrix) {
        this.basePolicy = basePolicy;
        this.matrix = matrix;
    }

    @Override
    public PolicyResult apply(int baseCents, List<DiscountCandidate> candidates) {
        PolicyResult baseResult = basePolicy.apply(baseCents, candidates);
        if (baseResult.getChosenCandidates().isEmpty()) {
            return baseResult;
        }

        List<DiscountCandidate> sorted = baseResult.getChosenCandidates().stream()
                .sorted(BEST_FIRST)
                .toList();

        List<DiscountCandidate> chosen = new ArrayList<>();
        List<RejectedCandidate> rejected = new ArrayList<>(baseResult.getRejectedCandidates());

        for (DiscountCandidate candidate : sorted) {
            boolean conflict = false;
            for (DiscountCandidate picked : chosen) {
                if (matrix.isConflict(candidate.getMutexGroup(), picked.getMutexGroup())) {
                    rejected.add(new RejectedCandidate(candidate,
                            "CONFLICT_WITH_GROUP:" + picked.getMutexGroup()));
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                chosen.add(candidate);
            }
        }

        int sum = chosen.stream().mapToInt(DiscountCandidate::getDiscountCents).sum();
        boolean capped = sum > baseCents;
        int finalDiscount = Math.min(sum, baseCents);
        return new PolicyResult(finalDiscount, chosen, rejected, capped);
    }
}
