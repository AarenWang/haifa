package org.wrj.haifa.leetcode;

/**
 * Created by wangrenjun on 2017/4/19.
 */
public class Solution152 {

    public int maxProduct(int[] nums) {
        if(nums.length == 1){
            return  nums[0];
        }

        int global = nums[0];
        int max = nums[0];
        int min = nums[0];

        int i = 1;
        while( i < nums.length){
            int a = nums[i] * max;
            int b = nums[i] * min;
            max = Math.max(nums[i],Math.max(a,b));
            min = Math.min(nums[i],Math.min(a,b));

            global = Math.max(max,global);
            i++;
        }

        return global;
    }
}
