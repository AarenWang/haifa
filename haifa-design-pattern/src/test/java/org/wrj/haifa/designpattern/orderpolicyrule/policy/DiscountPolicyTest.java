package org.wrj.haifa.designpattern.orderpolicyrule.policy;

import org.junit.jupiter.api.Test;
import org.wrj.haifa.designpattern.orderpolicyrule.model.ConflictMatrix;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountScope;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountPolicyTest {

    @Test
    void stackAllCapsAtBase() {
        DiscountPolicy policy = new StackAllPolicy();
        List<DiscountCandidate> candidates = List.of(
                candidate("A", "GROUP_A", 10, 300),
                candidate("B", "GROUP_B", 20, 500)
        );

        PolicyResult result = policy.apply(600, candidates);

        assertEquals(600, result.getAppliedDiscountCents());
        assertEquals(2, result.getChosenCandidates().size());
        assertTrue(result.isCapApplied());
    }

    @Test
    void takeMaxSelectsBestCandidate() {
        DiscountPolicy policy = new TakeMaxPolicy();
        List<DiscountCandidate> candidates = List.of(
                candidate("A", "GROUP_A", 10, 300),
                candidate("B", "GROUP_A", 20, 500),
                candidate("C", "GROUP_A", 5, 200)
        );

        PolicyResult result = policy.apply(400, candidates);

        assertEquals(400, result.getAppliedDiscountCents());
        assertEquals(1, result.getChosenCandidates().size());
        assertEquals("B", result.getChosenCandidates().get(0).getId());
        assertEquals(2, result.getRejectedCandidates().size());
    }

    @Test
    void mutexByGroupTakeMaxSelectsPerGroup() {
        DiscountPolicy policy = new MutexByGroupTakeMaxPolicy();
        List<DiscountCandidate> candidates = List.of(
                candidate("A1", "GROUP_A", 10, 300),
                candidate("A2", "GROUP_A", 20, 500),
                candidate("B1", "GROUP_B", 10, 200)
        );

        PolicyResult result = policy.apply(2000, candidates);

        assertEquals(700, result.getAppliedDiscountCents());
        assertEquals(2, result.getChosenCandidates().size());
        assertEquals(1, result.getRejectedCandidates().size());
    }

    @Test
    void conflictMatrixEliminatesConflictingGroups() {
        DiscountPolicy basePolicy = new MutexByGroupTakeMaxPolicy();
        ConflictMatrix matrix = new ConflictMatrix().addConflict("GROUP_A", "GROUP_B");
        DiscountPolicy policy = new ConflictMatrixPolicy(basePolicy, matrix);

        List<DiscountCandidate> candidates = List.of(
                candidate("A", "GROUP_A", 10, 500),
                candidate("B", "GROUP_B", 10, 400)
        );

        PolicyResult result = policy.apply(2000, candidates);

        assertEquals(500, result.getAppliedDiscountCents());
        assertEquals(1, result.getChosenCandidates().size());
        assertEquals("A", result.getChosenCandidates().get(0).getId());
        assertEquals(1, result.getRejectedCandidates().size());
        assertTrue(result.getRejectedCandidates().get(0).getReason().contains("GROUP_A"));
    }

    private DiscountCandidate candidate(String id, String group, int priority, int discountCents) {
        return DiscountCandidate.builder()
                .id(id)
                .scope(DiscountScope.ITEM)
                .mutexGroup(group)
                .priority(priority)
                .discountCents(discountCents)
                .stackable(false)
                .build();
    }
}
