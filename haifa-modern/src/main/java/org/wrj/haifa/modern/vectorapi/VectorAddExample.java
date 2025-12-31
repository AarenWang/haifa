package org.wrj.haifa.modern.vectorapi;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

public class VectorAddExample {
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        int length = 1000;
        int[] a = new int[length];
        int[] b = new int[length];
        int[] c = new int[length];
        for (int i = 0; i < length; i++) {
            a[i] = i;
            b[i] = length - i;
        }

        // Vectorized add
        int i = 0;
        int upper = SPECIES.loopBound(length);
        for (; i < upper; i += SPECIES.length()) {
            IntVector va = IntVector.fromArray(SPECIES, a, i);
            IntVector vb = IntVector.fromArray(SPECIES, b, i);
            IntVector vc = va.add(vb);
            vc.intoArray(c, i);
        }
        // Remainder
        for (; i < length; i++) {
            c[i] = a[i] + b[i];
        }

        // 简单校验
        for (int j = 0; j < length; j++) {
            if (c[j] != a[j] + b[j]) {
                throw new AssertionError("Mismatch at " + j);
            }
        }
        System.out.println("VectorAddExample: OK, length=" + length);
    }
}
