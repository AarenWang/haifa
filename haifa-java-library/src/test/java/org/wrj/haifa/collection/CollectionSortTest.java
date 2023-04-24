package org.wrj.haifa.collection;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionSortTest {


    @Test
    public void testSort() {

        List<Integer> list = new ArrayList<>();
        list.add(9);
        list.add(5);
        list.add(3);
        list.add(2);
        list.add(32);
        list.add(11);
        list.add(6);

        Collections.sort(list);
        for(int i = 0; i < list.size()-1; i++) {
            Assert.assertTrue(list.get(i) <= list.get(i+1));
        }
    }
}
