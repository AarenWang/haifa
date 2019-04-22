package org.wrj.haifa.dance;


import org.junit.Before;
import org.junit.Test;

public class ArrayAndSortTest {

    private ArrayAndSort arrayAndSort;

    @Before
    public void  before(){
        arrayAndSort = new ArrayAndSort();
    }

    @Test
    public  void testFindLengthOfLCIS(){
        System.out.println(arrayAndSort.findLengthOfLCIS(new int[]{1,3,5,4,7}));
        System.out.println(arrayAndSort.findLengthOfLCIS(new int[]{2,2,2,2,2}));
        System.out.println(arrayAndSort.findLengthOfLCIS(new int[]{1,3,5,6,2,9,33,55,56,67,23,33,45}));

    }

}
