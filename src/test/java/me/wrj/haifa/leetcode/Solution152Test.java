package me.wrj.haifa.leetcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by wangrenjun on 2017/4/19.
 */

public class Solution152Test {

    Solution152 solution152;
    @Before
    public  void init(){
        solution152 = new Solution152();
    }

    @Test
    public void  test(){
        Assert.assertEquals(solution152.maxProduct(new int[]{0}),0);
        Assert.assertEquals(solution152.maxProduct(new int[]{0,0}),0);
        Assert.assertEquals(solution152.maxProduct(new int[]{0,1}),1);
        Assert.assertEquals(solution152.maxProduct(new int[]{0,1,1}),1);
        Assert.assertEquals(solution152.maxProduct(new int[]{0,0,0,0,0}),0);
        Assert.assertEquals(solution152.maxProduct(new int[]{4,2,1,0,5,6,7,0,1,7,9,10}),630);
        Assert.assertEquals(solution152.maxProduct(new int[]{4,2,1,0,5,6,7,0,1,7,9,-10}),210);
        Assert.assertEquals(solution152.maxProduct(new int[]{-2}),-2);
    }

}
