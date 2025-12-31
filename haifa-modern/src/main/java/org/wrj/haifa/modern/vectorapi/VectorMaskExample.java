package org.wrj.haifa.modern.vectorapi;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public class VectorMaskExample {
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        int length = 10; // deliberately small to show tail handling
        int[] a = new int[length];
        int[] b = new int[length];
        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            a[i] = i + 1;
            b[i] = 2 * (i + 1);
        }

        int i = 0;
        int upper = SPECIES.loopBound(length);
        for (; i < upper; i += SPECIES.length()) {
            IntVector va = IntVector.fromArray(SPECIES, a, i);
            IntVector vb = IntVector.fromArray(SPECIES, b, i);
            IntVector vc = va.add(vb);
            vc.intoArray(out, i);
        }

        if (i < length) {
            VectorMask<Integer> m = SPECIES.indexInRange(i, length);
            IntVector va = IntVector.fromArray(SPECIES, a, i, m);
            IntVector vb = IntVector.fromArray(SPECIES, b, i, m);
            IntVector vc = va.add(vb);
            vc.intoArray(out, i, m);
        }

        // 校验
        for (int j = 0; j < length; j++) {
            if (out[j] != a[j] + b[j]) {
                throw new AssertionError("Mask mismatch at " + j);
            }
        }
        System.out.println("VectorMaskExample: OK, length=" + length);
    }
}
