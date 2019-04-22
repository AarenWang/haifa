package org.wrj.haifa.dance;

public class ArrayAndSort {

    public int findLengthOfLCIS(int[] nums) {

        int current = 1;
        int max = 1;
        for(int i = 0; i < nums.length-1; i++) {
           if(nums[i] < nums[i+1]) {
               current++;
           } else {
               if(current > max) {
                   max = current;
               }
               current = 1;
           }
        }
        return max;
    }


}
