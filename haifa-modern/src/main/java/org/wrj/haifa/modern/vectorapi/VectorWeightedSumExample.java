package org.wrj.haifa.modern.vectorapi;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

public class VectorWeightedSumExample {
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        int length = 100;
        int[] values = new int[length];
        int[] weights = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = i + 1;
            weights[i] = (i % 3) + 1;
        }

        long acc = 0L;
        int i = 0;
        int upper = SPECIES.loopBound(length);
        for (; i < upper; i += SPECIES.length()) {
            IntVector v = IntVector.fromArray(SPECIES, values, i);
            IntVector w = IntVector.fromArray(SPECIES, weights, i);
            IntVector prod = v.mul(w);
            acc += prod.reduceLanes(jdk.incubator.vector.VectorOperators.ADD);
        }
        for (; i < length; i++) {
            acc += (long) values[i] * weights[i];
        }

        // 校验（标量计算）
        long expected = 0L;
        for (int j = 0; j < length; j++) expected += (long) values[j] * weights[j];
        if (acc != expected) throw new AssertionError("Weighted sum mismatch");
        System.out.println("VectorWeightedSumExample: OK, sum=" + acc);
    }
}
