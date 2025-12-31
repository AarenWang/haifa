package org.wrj.haifa.modern.vectorapi;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class VectorReduceExample {
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        int length = 1023;
        int[] a = new int[length];
        for (int i = 0; i < length; i++) {
            a[i] = 1; // sum should be length
        }

        int sum = 0;
        int i = 0;
        int upper = SPECIES.loopBound(length);
        for (; i < upper; i += SPECIES.length()) {
            IntVector va = IntVector.fromArray(SPECIES, a, i);
            sum += va.reduceLanes(VectorOperators.ADD);
        }
        for (; i < length; i++) {
            sum += a[i];
        }

        if (sum != length) {
            throw new AssertionError("Reduce mismatch: " + sum);
        }
        System.out.println("VectorReduceExample: OK, sum=" + sum);
    }
}
